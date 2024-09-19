package ml.jk.android.budgetapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsActivity : AppCompatActivity() {

    // Referencje do widoków
    private lateinit var minAmountText: TextView
    private lateinit var newAmountInput: EditText
    private lateinit var changeMinButton: Button
    private lateinit var returnHomeButton: Button

    // Instancje Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var minAmount: Double = 100.0 // Domyślna minimalna kwota

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Znajdź widoki po ich ID
        minAmountText = findViewById(R.id.minimumAmountText)
        newAmountInput = findViewById(R.id.newAmountInput)
        changeMinButton = findViewById(R.id.changeMinButton)
        returnHomeButton = findViewById(R.id.returnHomeButton)

        // Wyświetlanie domyślnej kwoty minimalnej
        minAmountText.text = "MINIMUM AMOUNT: $minAmount"

        // Tworzymy kanał powiadomień (dla Android O i wyżej)
        createNotificationChannel()

        // Ustaw akcję przycisku zmiany minimalnej kwoty
        changeMinButton.setOnClickListener {
            // Pobranie nowej kwoty z pola wejściowego i aktualizacja
            val newAmount = newAmountInput.text.toString().toDoubleOrNull()
            if (newAmount != null) {
                minAmount = newAmount
                minAmountText.text = "MINIMUM AMOUNT: $minAmount"
                saveMinAmountToFirebase(minAmount) // Zapisz nową kwotę do Firestore
                Toast.makeText(this, "Minimum amount updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        // Ustaw akcję przycisku powrotu do ekranu głównego
        returnHomeButton.setOnClickListener {
            finish() // Zakończ aktywność i wróć do poprzedniego ekranu
        }

        // Rozpocznij monitorowanie zmian w saldzie użytkownika
        monitorBalanceChanges()
    }

    // Zapisanie minimalnej kwoty do Firestore
    private fun saveMinAmountToFirebase(minAmount: Double) {
        val userId = auth.currentUser?.uid ?: return
        // Aktualizacja dokumentu użytkownika w kolekcji "users"
        db.collection("users").document(userId).update("minAmount", minAmount)
            .addOnSuccessListener {
                Toast.makeText(this, "Min amount saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save min amount", Toast.LENGTH_SHORT).show()
            }
    }

    // Monitorowanie zmian w saldzie użytkownika
    private fun monitorBalanceChanges() {
        val userId = auth.currentUser?.uid ?: return
        // Nasłuchuj zmiany w dokumencie użytkownika
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Toast.makeText(this, "Failed to monitor balance", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val currentBalance = snapshot.getDouble("balance") ?: 0.0
                val minAmount = snapshot.getDouble("minAmount") ?: this.minAmount

                // Jeśli saldo jest mniejsze lub równe ustalonej kwocie minimalnej, wyślij powiadomienie
                if (currentBalance <= minAmount) {
                    sendNotification(currentBalance, minAmount)
                }
            }
    }

    // Wysłanie powiadomienia, gdy saldo spadnie poniżej minimalnej kwoty
    private fun sendNotification(balance: Double, minAmount: Double) {
        val notificationBuilder = NotificationCompat.Builder(this, "balanceChannel")
            .setSmallIcon(R.drawable.ic_notification) // Ikona powiadomienia
            .setContentTitle("Low Balance Alert")
            .setContentText("Your balance ($balance PLN) is lower than the minimum set amount ($minAmount PLN).")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ustaw priorytet powiadomienia na wysoki

        // Zarządzanie powiadomieniami
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, notificationBuilder.build()) // Wyślij powiadomienie z ID 1001
    }

    // Tworzymy kanał powiadomień (dla Android O i wyżej)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Balance Notifications"
            val descriptionText = "Notifies when balance is below the set minimum"
            val importance = NotificationManager.IMPORTANCE_HIGH // Ustawienie ważności kanału
            val channel = NotificationChannel("balanceChannel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
