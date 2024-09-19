package ml.jk.android.budgetapp

import Transaction
import TransactionsAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TransactionsActivity : AppCompatActivity() {

    private lateinit var transactionsRecyclerView: RecyclerView // RecyclerView do wyświetlania listy transakcji
    private lateinit var adapter: TransactionsAdapter // Adapter do powiązania danych z RecyclerView
    private lateinit var sortSpinner: Spinner // Spinner do wyboru opcji sortowania
    private val transactionsList = mutableListOf<Transaction>() // Lista transakcji do wyświetlenia
    private val db = FirebaseFirestore.getInstance() // Instancja Firestore do interakcji z bazą danych
    private val auth = FirebaseAuth.getInstance() // Instancja FirebaseAuth do zarządzania uwierzytelnieniem użytkownika

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions) // Ustawienie widoku z layoutu

        // Inicjalizacja RecyclerView i ustawienie menedżera układu
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Inicjalizacja adaptera i przypisanie go do RecyclerView
        adapter = TransactionsAdapter(transactionsList) { transaction ->
            deleteTransaction(transaction) // Usuwanie transakcji po kliknięciu
        }
        transactionsRecyclerView.adapter = adapter

        // Inicjalizacja spinnera z opcjami sortowania
        sortSpinner = findViewById(R.id.sortSpinner)

        // Ustawienie adaptera dla spinnera sortowania
        val sortOptions = arrayOf("Kwota: Od najwyższej", "Kwota: Od najniższej", "Data", "Kategoria: Alfabetycznie")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = spinnerAdapter

        // Ustawienie nasłuchiwacza dla wyboru opcji sortowania w spinnerze
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> fetchTransactions(Query.Direction.DESCENDING, "amount") // Kwota: Od najwyższej
                    1 -> fetchTransactions(Query.Direction.ASCENDING, "amount") // Kwota: Od najniższej
                    2 -> fetchTransactions(Query.Direction.DESCENDING, "timestamp") // Data
                    3 -> fetchTransactions(Query.Direction.ASCENDING, "category") // Kategoria: Alfabetycznie
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Ustawienie akcji przycisku dodawania transakcji
        findViewById<Button>(R.id.addTransactionButton).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java)) // Przejście do aktywności dodawania transakcji
        }

        // Początkowe pobranie transakcji (domyślnie sortowane po dacie)
        fetchTransactions(Query.Direction.DESCENDING, "timestamp")
    }

    // Funkcja nasłuchująca na zmiany w kolekcji transakcji
    private fun fetchTransactions(direction: Query.Direction, orderByField: String) {
        val userId = auth.currentUser?.uid ?: return // Pobranie identyfikatora aktualnie zalogowanego użytkownika

        db.collection("users").document(userId).collection("transactions")
            .orderBy(orderByField, direction) // Sortowanie wyników według podanego pola i kierunku
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Failed to load transactions", Toast.LENGTH_SHORT).show() // Wyświetlenie błędu w przypadku niepowodzenia
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    transactionsList.clear() // Wyczyść istniejącą listę transakcji
                    for (document in snapshot.documents) {
                        // Pobranie danych transakcji z dokumentu
                        val amount = document.getDouble("amount") ?: 0.0
                        val category = document.getString("category") ?: ""
                        val type = document.getString("type") ?: ""
                        val description = document.getString("description") ?: ""
                        val timestamp = document.getLong("timestamp") ?: 0L
                        val id = document.id // Pobranie identyfikatora dokumentu
                        transactionsList.add(Transaction(id, amount, category, type, description, timestamp))
                    }
                    adapter.notifyDataSetChanged() // Odświeżenie listy w RecyclerView
                } else {
                    transactionsList.clear() // Wyczyść listę, jeśli brak danych
                    adapter.notifyDataSetChanged()
                    //Toast.makeText(this, "No transactions found", Toast.LENGTH_SHORT).show() // (Opcjonalnie) Wyświetlenie komunikatu, gdy brak transakcji
                }
            }
    }

    // Funkcja usuwająca transakcję
    private fun deleteTransaction(transaction: Transaction) {
        val userId = auth.currentUser?.uid ?: return // Pobranie identyfikatora aktualnie zalogowanego użytkownika

        // Usuwanie transakcji na podstawie jej id
        db.collection("users").document(userId).collection("transactions")
            .document(transaction.id)
            .delete()
            .addOnSuccessListener {
                transactionsList.remove(transaction) // Usunięcie transakcji z listy
                adapter.notifyDataSetChanged() // Odświeżenie listy w RecyclerView
                updateBalance(userId, transaction.amount) // Zaktualizowanie salda po usunięciu transakcji
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete transaction", Toast.LENGTH_SHORT).show() // Wyświetlenie błędu, jeśli usunięcie się nie powiodło
            }
    }

    // Funkcja aktualizująca saldo użytkownika
    private fun updateBalance(userId: String, amount: Double) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            val currentBalance = document.getDouble("balance") ?: 0.0 // Pobranie aktualnego salda
            val newBalance = currentBalance + amount // Dodanie kwoty usuniętej transakcji do salda

            userDocRef.update("balance", newBalance)
                .addOnSuccessListener {
                    Toast.makeText(this, "Balance updated", Toast.LENGTH_SHORT).show() // Wyświetlenie komunikatu o udanej aktualizacji salda
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update balance", Toast.LENGTH_SHORT).show() // Wyświetlenie błędu, jeśli aktualizacja salda się nie powiodła
                }
        }
    }
}
