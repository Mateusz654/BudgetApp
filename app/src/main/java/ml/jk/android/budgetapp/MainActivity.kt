package ml.jk.android.budgetapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.*

class MainActivity : AppCompatActivity() {

    // Referencje do widoków
    private lateinit var welcomeText: TextView
    private lateinit var currentBalanceText: TextView
    private lateinit var totalExpensesText: TextView
    private lateinit var totalRevenueText: TextView
    private lateinit var recentTransactionsText: TextView

    // Instancje Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Referencje do nasłuchiwaczy
    private var balanceListener: ListenerRegistration? = null
    private var expensesListener: ListenerRegistration? = null
    private var revenueListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Znajdź widoki po ich ID
        welcomeText = findViewById(R.id.welcomeText)
        currentBalanceText = findViewById(R.id.currentBalanceText)
        totalExpensesText = findViewById(R.id.totalExpensesText)
        totalRevenueText = findViewById(R.id.totalRevenueText)
        recentTransactionsText = findViewById(R.id.recentTransactionsText)

        // Znajdź przyciski po ich ID
        val transactionsButton = findViewById<Button>(R.id.transactionsButton)
        val categoriesButton = findViewById<Button>(R.id.categoriesButton)
        val reportsButton = findViewById<Button>(R.id.reportsButton)
        val notificationsButton = findViewById<Button>(R.id.notificationsButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val addTransactionButton = findViewById<Button>(R.id.addTransactionButton)
        val addDonate = findViewById<Button>(R.id.donateButton)

        // Ustaw akcje przycisków
        transactionsButton.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }
        addDonate.setOnClickListener {
            startActivity(Intent(this, DonateActivity::class.java))
        }
        categoriesButton.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }
        reportsButton.setOnClickListener {
            startActivity(Intent(this, RaportsActivity::class.java))
        }
        notificationsButton.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        logoutButton.setOnClickListener {
            // Wylogowanie użytkownika i przekierowanie do aktywności logowania
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        addTransactionButton.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        // Wyświetl nazwę użytkownika lub e-mail
        displayUserName()
    }

    override fun onStart() {
        super.onStart()
        // Rozpoczęcie nasłuchiwania zmian w saldzie użytkownika
        startListeningToBalance()
        // Rozpoczęcie nasłuchiwania zmian w wydatkach użytkownika
        startListeningToExpenses()
        // Rozpoczęcie nasłuchiwania zmian w przychodach użytkownika
        startListeningToRevenue()
        // Rozpoczęcie nasłuchiwania na ostatnie transakcje
        startListeningToRecentTransactions()
    }

    override fun onStop() {
        super.onStop()
        // Zatrzymaj nasłuchiwanie zmian, aby uniknąć wycieków pamięci
        balanceListener?.remove()
        expensesListener?.remove()
        revenueListener?.remove()
        transactionsListener?.remove()
    }

    // Funkcja wyświetlająca nazwę użytkownika lub jego email
    private fun displayUserName() {
        val currentUser = auth.currentUser
        // Pobranie nazwy użytkownika lub emaila, jeśli nazwa nie jest ustawiona
        val userName = currentUser?.displayName ?: currentUser?.email
        welcomeText.text = "Hello, ${userName ?: "User"}!"
    }

    // Nasłuchiwanie na zmiany w saldzie
    private fun startListeningToBalance() {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)

        balanceListener = userDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                currentBalanceText.text = "CURRENT BALANCE: Error"
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val balance = snapshot.getDouble("balance") ?: 0.0
                currentBalanceText.text = "CURRENT BALANCE: $balance PLN"
            } else {
                currentBalanceText.text = "CURRENT BALANCE: 0 PLN"
            }
        }
    }

    // Nasłuchiwanie na zmiany w wydatkach
    private fun startListeningToExpenses() {
        val userId = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1) // Ustawienie daty na miesiąc wstecz
        val lastMonthTimestamp = calendar.timeInMillis

        expensesListener = db.collection("users").document(userId).collection("donations")
            .whereGreaterThan("timestamp", lastMonthTimestamp) // Filtruj dokumenty po dacie
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    totalExpensesText.text = "TOTAL EXPENSES: Error"
                    return@addSnapshotListener
                }

                var totalExpenses = 0.0
                for (doc in snapshots!!) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    totalExpenses += amount
                }
                totalExpensesText.text = "TOTAL EXPENSES: $totalExpenses PLN"
            }
    }

    // Nasłuchiwanie na zmiany w przychodach
    private fun startListeningToRevenue() {
        val userId = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1) // Ustawienie daty na miesiąc wstecz
        val lastMonthTimestamp = calendar.timeInMillis

        revenueListener = db.collection("users").document(userId).collection("transactions")
            .whereGreaterThan("timestamp", lastMonthTimestamp) // Filtruj dokumenty po dacie
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    totalRevenueText.text = "TOTAL REVENUE: Error"
                    return@addSnapshotListener
                }

                var totalRevenue = 0.0
                for (doc in snapshots!!) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    totalRevenue += amount
                }
                totalRevenueText.text = "TOTAL REVENUE: $totalRevenue PLN"
            }
    }

    // Nasłuchiwanie na ostatnie transakcje
    private fun startListeningToRecentTransactions() {
        val userId = auth.currentUser?.uid ?: return

        transactionsListener = db.collection("users").document(userId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Sortowanie według daty malejąco
            .limit(5) // Ograniczenie do 5 najnowszych transakcji
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    recentTransactionsText.text = "RECENT TRANSACTIONS: Error"
                    return@addSnapshotListener
                }

                val transactions = StringBuilder("RECENT TRANSACTIONS:\n")
                for (doc in snapshots!!) {
                    val category = doc.getString("category") ?: "Unknown"
                    val amount = doc.getDouble("amount") ?: 0.0
                    transactions.append("- $category: $amount PLN\n")
                }
                recentTransactionsText.text = transactions.toString()
            }
    }
}
