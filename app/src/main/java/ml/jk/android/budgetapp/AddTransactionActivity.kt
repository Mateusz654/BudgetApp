package ml.jk.android.budgetapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddTransactionActivity : AppCompatActivity() {

    // Deklaracja zmiennych dla widoków UI
    private lateinit var amountInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var addTransactionButton: Button
    private lateinit var returnHomeButton: Button
    private lateinit var categorySpinner: Spinner

    // Deklaracja zmiennych dla Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val categories = arrayListOf<String>() // Lista kategorii

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction) // Ustawienie widoku dla tej aktywności

        // Inicjalizacja widoków
        amountInput = findViewById(R.id.amountInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        addTransactionButton = findViewById(R.id.addTransactionButton)
        returnHomeButton = findViewById(R.id.returnHomeButton)
        categorySpinner = findViewById(R.id.categorySpinner) // Spinner do wyboru kategorii

        // Inicjalizacja Firebase Auth i Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Pobierz kategorie z Firestore i ustaw spinner
        fetchCategories()

        // Obsługa kliknięcia przycisku dodawania transakcji
        addTransactionButton.setOnClickListener {
            val amountText = amountInput.text.toString()
            val description = descriptionInput.text.toString()
            val selectedCategory = categorySpinner.selectedItem?.toString()

            if (amountText.isNotEmpty() && description.isNotEmpty() && selectedCategory != null) {
                val amount = amountText.toDoubleOrNull() // Przekształcenie tekstu na liczbę
                if (amount != null) {
                    addTransaction(amount, description, selectedCategory)
                    // Wyczyść pola po dodaniu transakcji
                    amountInput.text.clear()
                    descriptionInput.text.clear()
                } else {
                    // Wyświetlenie komunikatu o błędzie, jeśli kwota jest nieprawidłowa
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Wyświetlenie komunikatu o błędzie, jeśli nie wszystkie pola są wypełnione
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Obsługa kliknięcia przycisku powrotu do ekranu głównego
        returnHomeButton.setOnClickListener {
            finish() // Zamyka bieżącą aktywność
        }
    }

    // Funkcja pobierająca kategorie z Firestore
    private fun fetchCategories() {
        val userId = auth.currentUser?.uid ?: return // Pobranie ID aktualnego użytkownika
        db.collection("users").document(userId).collection("categories").get()
            .addOnSuccessListener { result ->
                categories.clear() // Wyczyść istniejące kategorie
                for (document in result) {
                    val category = document.getString("name")
                    category?.let { categories.add(it) } // Dodaj kategorię do listy, jeśli nie jest pusta
                }
                setupCategorySpinner() // Ustaw spinner po pobraniu kategorii
            }
            .addOnFailureListener {
                // Wyświetlenie komunikatu o błędzie, jeśli pobieranie kategorii się nie powiodło
                Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
    }

    // Funkcja konfigurująca spinner kategorii
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter // Ustaw adapter dla spinnera
    }

    // Funkcja dodająca transakcję do Firestore
    private fun addTransaction(amount: Double, description: String, category: String) {
        val userId = auth.currentUser?.uid ?: return // Pobranie ID aktualnego użytkownika

        // Stworzenie obiektu transakcji
        val transaction = hashMapOf(
            "amount" to amount,
            "description" to description,
            "category" to category,
            "timestamp" to System.currentTimeMillis() // Dodanie znacznika czasu
        )

        // Dodanie transakcji do Firestore
        db.collection("users").document(userId).collection("transactions").add(transaction)
            .addOnSuccessListener {
                // Aktualizacja salda po dodaniu transakcji
                updateBalance(userId, -amount)  // Odejmowanie kwoty
            }
            .addOnFailureListener {
                // Wyświetlenie komunikatu o błędzie, jeśli dodawanie transakcji się nie powiodło
                Toast.makeText(this, "Failed to add transaction", Toast.LENGTH_SHORT).show()
            }
    }

    // Funkcja aktualizująca saldo użytkownika
    private fun updateBalance(userId: String, amount: Double) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            val currentBalance = document.getDouble("balance") ?: 0.0
            val newBalance = currentBalance + amount // Aktualizacja salda

            userDocRef.update("balance", newBalance)
                .addOnSuccessListener {
                    // Wyświetlenie komunikatu o pomyślnej aktualizacji salda
                    Toast.makeText(this, "Transaction added and balance updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Wyświetlenie komunikatu o błędzie, jeśli aktualizacja salda się nie powiodła
                    Toast.makeText(this, "Failed to update balance", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
