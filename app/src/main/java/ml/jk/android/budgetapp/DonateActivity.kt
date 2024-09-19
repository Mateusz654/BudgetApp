package ml.jk.android.budgetapp

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class DonateActivity : AppCompatActivity() {

    // Deklaracja zmiennych dla widoków UI
    private lateinit var donationAmountInput: EditText
    private lateinit var addDonationButton: Button
    private lateinit var returnHomeButton: Button
    private lateinit var donationsList: LinearLayout // Kontener do wyświetlania donacji

    // Deklaracja instancji Firebase Auth i Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate) // Ustawienie widoku dla tej aktywności

        // Inicjalizacja widoków
        donationAmountInput = findViewById(R.id.donationAmountInput)
        addDonationButton = findViewById(R.id.addDonationButton)
        returnHomeButton = findViewById(R.id.returnHomeButton)
        donationsList = findViewById(R.id.donationsList) // Kontener dla donacji

        // Inicjalizacja instancji Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Załaduj donacje użytkownika przy uruchomieniu aktywności
        loadDonations()

        // Obsługa kliknięcia przycisku dodawania donacji
        addDonationButton.setOnClickListener {
            val amountText = donationAmountInput.text.toString()

            // Sprawdzenie, czy pole tekstowe nie jest puste
            if (amountText.isNotEmpty()) {
                val amount = amountText.toDoubleOrNull() // Konwersja tekstu na liczbę
                if (amount != null) {
                    addDonation(amount) // Dodanie donacji
                    donationAmountInput.text.clear() // Wyczyść pole tekstowe po dodaniu
                } else {
                    // Wyświetlenie komunikatu o błędzie, jeśli kwota jest nieprawidłowa
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Wyświetlenie komunikatu, jeśli pole tekstowe jest puste
                Toast.makeText(this, "Please enter a donation amount", Toast.LENGTH_SHORT).show()
            }
        }

        // Obsługa kliknięcia przycisku powrotu do poprzedniego ekranu
        returnHomeButton.setOnClickListener {
            finish() // Zakończenie aktywności i powrót do poprzedniego ekranu
        }
    }

    // Funkcja dodająca nową donację
    private fun addDonation(amount: Double) {
        val userId = auth.currentUser?.uid ?: return // Pobranie ID aktualnego użytkownika

        // Stworzenie obiektu donacji
        val donation = hashMapOf(
            "amount" to amount,
            "timestamp" to System.currentTimeMillis() // Aktualny czas jako timestamp
        )

        // Dodanie donacji do Firestore
        db.collection("users").document(userId).collection("donations").add(donation)
            .addOnSuccessListener {
                // Aktualizacja salda użytkownika i ponowne załadowanie donacji
                updateBalance(userId, amount)
                loadDonations()
            }
            .addOnFailureListener {
                // Wyświetlenie komunikatu o błędzie, jeśli dodawanie donacji się nie powiodło
                Toast.makeText(this, "Failed to add donation", Toast.LENGTH_SHORT).show()
            }
    }

    // Funkcja aktualizująca saldo użytkownika
    private fun updateBalance(userId: String, amount: Double) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            val currentBalance = document.getDouble("balance") ?: 0.0 // Pobranie aktualnego salda
            val newBalance = currentBalance + amount // Dodanie kwoty donacji do salda

            // Aktualizacja salda w Firestore
            userDocRef.update("balance", newBalance)
                .addOnSuccessListener {
                    // Wyświetlenie komunikatu o pomyślnej aktualizacji salda
                    Toast.makeText(this, "Donation added and balance updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Wyświetlenie komunikatu o błędzie, jeśli aktualizacja salda się nie powiodła
                    Toast.makeText(this, "Failed to update balance", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Funkcja pobierająca donacje z Firestore i wyświetlająca je w widoku
    private fun loadDonations() {
        val userId = auth.currentUser?.uid ?: return // Pobranie ID aktualnego użytkownika

        // Pobranie kolekcji donacji z Firestore
        db.collection("users").document(userId).collection("donations").get()
            .addOnSuccessListener { result ->
                donationsList.removeAllViews() // Oczyść widok przed dodaniem nowych donacji

                for (document in result) {
                    val donationId = document.id // ID donacji
                    val amount = document.getDouble("amount") ?: 0.0 // Kwota donacji
                    val timestamp = document.getLong("timestamp") ?: 0L // Timestamp donacji

                    // Konwersja timestamp na formatowaną datę
                    val donationDate = DateFormat.format("yyyy-MM-dd HH:mm", Date(timestamp)).toString()

                    // Dynamiczne tworzenie widoku dla pojedynczej donacji
                    val donationLayout = LinearLayout(this)
                    donationLayout.orientation = LinearLayout.HORIZONTAL
                    donationLayout.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    // TextView z kwotą donacji i datą
                    val donationView = TextView(this)
                    donationView.text = "Donation: $amount PLN | Date: $donationDate"
                    donationView.layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                    )
                    donationView.textSize = 18f // Ustawienie rozmiaru czcionki

                    // Przycisk usunięcia donacji
                    val deleteButton = ImageButton(this)
                    deleteButton.setImageResource(android.R.drawable.ic_delete)
                    deleteButton.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    // Usunięcie donacji po kliknięciu przycisku
                    deleteButton.setOnClickListener {
                        removeDonation(userId, donationId, amount, donationLayout)
                    }

                    // Dodanie widoków do layoutu donacji
                    donationLayout.addView(donationView)
                    donationLayout.addView(deleteButton)
                    donationsList.addView(donationLayout) // Dodanie donacji do kontenera
                }
            }
            .addOnFailureListener {
                // Wyświetlenie komunikatu o błędzie, jeśli pobieranie donacji się nie powiodło
                Toast.makeText(this, "Failed to load donations", Toast.LENGTH_SHORT).show()
            }
    }

    // Funkcja usuwająca donację z Firestore i aktualizująca saldo
    private fun removeDonation(userId: String, donationId: String, amount: Double, donationLayout: View) {
        db.collection("users").document(userId).collection("donations").document(donationId)
            .delete()
            .addOnSuccessListener {
                // Aktualizacja salda po usunięciu donacji
                updateBalanceAfterRemoval(userId, amount)
                donationsList.removeView(donationLayout) // Usunięcie widoku donacji
                Toast.makeText(this, "Donation removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Wyświetlenie komunikatu o błędzie, jeśli usuwanie donacji się nie powiodło
                Toast.makeText(this, "Failed to remove donation", Toast.LENGTH_SHORT).show()
            }
    }

    // Funkcja aktualizująca saldo po usunięciu donacji
    private fun updateBalanceAfterRemoval(userId: String, amount: Double) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            val currentBalance = document.getDouble("balance") ?: 0.0 // Pobranie aktualnego salda
            val newBalance = currentBalance - amount // Odejmowanie kwoty usuniętej donacji od salda

            // Aktualizacja salda w Firestore
            userDocRef.update("balance", newBalance)
                .addOnSuccessListener {
                    // Wyświetlenie komunikatu o pomyślnej aktualizacji salda
                    Toast.makeText(this, "Balance updated after donation removal", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Wyświetlenie komunikatu o błędzie, jeśli aktualizacja salda się nie powiodła
                    Toast.makeText(this, "Failed to update balance", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
