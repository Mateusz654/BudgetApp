package ml.jk.android.budgetapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CategoriesActivity : AppCompatActivity() {

    // Deklaracja zmiennych dla widoków UI
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoriesAdapter
    private val categories = arrayListOf<String>() // Lista kategorii do wyświetlenia
    private val db = FirebaseFirestore.getInstance() // Instancja Firestore
    private val auth = FirebaseAuth.getInstance() // Instancja FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories) // Ustawienie widoku dla tej aktywności

        // Inicjalizacja widoków
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton)
        val categoryInput = findViewById<EditText>(R.id.categoryInput)

        // Adapter do wyświetlania kategorii w RecyclerView
        categoryAdapter = CategoriesAdapter(categories) { category ->
            removeCategory(category) // Usuwanie kategorii po kliknięciu
        }
        categoryRecyclerView.adapter = categoryAdapter
        categoryRecyclerView.layoutManager = LinearLayoutManager(this) // Ustawienie układu listy

        // Pobieranie kategorii z Firestore
        fetchCategories()

        // Dodawanie nowej kategorii
        addCategoryButton.setOnClickListener {
            val newCategory = categoryInput.text.toString()
            if (newCategory.isNotEmpty()) {
                addCategory(newCategory) // Dodanie kategorii do Firestore
                categoryInput.text.clear() // Wyczyść pole tekstowe po dodaniu
            } else {
                // Wyświetlenie komunikatu, jeśli pole tekstowe jest puste
                Toast.makeText(this, "Category cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Obsługa kliknięcia przycisku powrotu do HomeActivity
        val homeButton = findViewById<Button>(R.id.returnHomeButton)
        homeButton.setOnClickListener {
            finish() // Zakończenie aktywności i powrót do poprzedniego ekranu
        }
    }

    // Funkcja pobierająca kategorie z Firestore
    private fun fetchCategories() {
        val userId = auth.currentUser?.uid ?: return // Pobranie ID aktualnego użytkownika
        db.collection("users").document(userId).collection("categories").get()
            .addOnSuccessListener { result ->
                categories.clear() // Wyczyść listę kategorii przed dodaniem nowych
                for (document in result) {
                    val categoryName = document.getString("name")
                    categoryName?.let { categories.add(it) } // Dodaj kategorię do listy
                }
                categoryAdapter.notifyDataSetChanged() // Odświeżenie widoku listy
            }
            .addOnFailureListener { e ->
                // Wyświetlenie komunikatu o błędzie, jeśli pobieranie kategorii się nie powiodło
                Toast.makeText(this, "Failed to load categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Funkcja dodająca nową kategorię do Firestore
    private fun addCategory(category: String) {
        val userId = auth.currentUser?.uid ?: return // Pobranie ID aktualnego użytkownika
        val categoryMap = hashMapOf("name" to category) // Stworzenie obiektu z kategorią
        db.collection("users").document(userId).collection("categories").add(categoryMap)
            .addOnSuccessListener {
                categories.add(category) // Dodanie kategorii do lokalnej listy
                categoryAdapter.notifyDataSetChanged() // Odświeżenie widoku listy
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Wyświetlenie komunikatu o błędzie, jeśli dodawanie kategorii się nie powiodło
                Toast.makeText(this, "Failed to add category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Funkcja usuwająca kategorię z Firestore
    private fun removeCategory(category: String) {
        val userId = auth.currentUser?.uid ?: return // Pobranie ID aktualnego użytkownika
        db.collection("users").document(userId).collection("categories").whereEqualTo("name", category).get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    db.collection("users").document(userId).collection("categories").document(document.id).delete()
                }
                categories.remove(category) // Usunięcie kategorii z lokalnej listy
                categoryAdapter.notifyDataSetChanged() // Odświeżenie widoku listy
                Toast.makeText(this, "Category removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Wyświetlenie komunikatu o błędzie, jeśli usuwanie kategorii się nie powiodło
                Toast.makeText(this, "Failed to remove category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
