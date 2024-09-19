package ml.jk.android.budgetapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Włącza tryb edge-to-edge, aby treść wyświetlała się od krawędzi do krawędzi ekranu
        //enableEdgeToEdge()
        // Ustawienie treści widoku z pomocą Compose
        setContent {
            RegisterView()
        }
    }
}

// Komponent widoku rejestracji użytkownika
@Composable
fun RegisterView() {
    val context = LocalContext.current // Uzyskuje kontekst lokalny, używany do wywoływania Toastów i Intentów
    // Stan dla pól wejściowych
    var login by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password1 by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }

    // Kolumna z widokami
    Column(
        modifier = Modifier
            .statusBarsPadding() // Padding dla status bar
            .background(Color(0xffeb9234)) // Tło widoku
            .padding(10.dp) // Padding wewnętrzny
            .fillMaxSize(), // Wypełnia cały dostępny rozmiar
        verticalArrangement = Arrangement.Center, // Wyrównanie pionowe do środka
        horizontalAlignment = Alignment.CenterHorizontally // Wyrównanie poziome do środka
    ) {
        // Tytuł formularza rejestracji
        Text(
            text = "REGISTER",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp) // Padding wokół tekstu
        )
        Spacer(modifier = Modifier.height(30.dp)) // Przerwa między elementami
        // Pole tekstowe dla loginu
        TextField(
            modifier = Modifier.background(Color.White),
            value = login,
            onValueChange = { login = it }, // Aktualizacja stanu loginu
            textStyle = TextStyle(fontSize = 20.sp),
            label = { Text("Login") } // Etykieta dla pola tekstowego
        )
        Spacer(modifier = Modifier.height(20.dp)) // Przerwa między elementami
        // Pole tekstowe dla e-maila
        TextField(
            modifier = Modifier.background(Color.White),
            value = email,
            onValueChange = { email = it }, // Aktualizacja stanu e-maila
            textStyle = TextStyle(fontSize = 20.sp),
            label = { Text("Email") } // Etykieta dla pola tekstowego
        )
        Spacer(modifier = Modifier.height(20.dp)) // Przerwa między elementami
        // Pole tekstowe dla hasła
        TextField(
            modifier = Modifier.background(Color.White),
            value = password1,
            onValueChange = { password1 = it }, // Aktualizacja stanu hasła
            textStyle = TextStyle(fontSize = 20.sp),
            label = { Text("Password") }, // Etykieta dla pola tekstowego
            visualTransformation = PasswordVisualTransformation() // Ukrywa tekst hasła
        )
        Spacer(modifier = Modifier.height(20.dp)) // Przerwa między elementami
        // Pole tekstowe dla potwierdzenia hasła
        TextField(
            modifier = Modifier.background(Color.White),
            value = password2,
            onValueChange = { password2 = it }, // Aktualizacja stanu potwierdzenia hasła
            textStyle = TextStyle(fontSize = 20.sp),
            label = { Text("Confirm Password") }, // Etykieta dla pola tekstowego
            visualTransformation = PasswordVisualTransformation() // Ukrywa tekst hasła
        )
        Spacer(modifier = Modifier.height(30.dp)) // Przerwa między elementami
        // Sprawdzenie, czy hasła się zgadzają
        if (password1 != password2) {
            Text(text = "Passwords don't match!", color = Color.Red, fontSize = 18.sp) // Wyświetla błąd, jeśli hasła się nie zgadzają
        }
        Spacer(modifier = Modifier.height(20.dp)) // Przerwa między elementami
        // Przycisk rejestracji
        Button(
            onClick = {
                if (password1 == password2) {
                    // Tworzenie nowego użytkownika w Firebase Authentication
                    Firebase.auth.createUserWithEmailAndPassword(email, password1)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = Firebase.auth.currentUser
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = login // Ustawienie nazwy użytkownika
                                }
                                user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        val userMap = hashMapOf(
                                            "login" to login,
                                            "email" to email
                                        )
                                        // Zapisanie danych użytkownika do Firestore
                                        FirebaseFirestore.getInstance().collection("users")
                                            .document(user.uid)
                                            .set(userMap)
                                            .addOnSuccessListener {
                                                // Przejście do aktywności logowania
                                                val intent = Intent(context, LoginActivity::class.java)
                                                context.startActivity(intent)
                                            }
                                            .addOnFailureListener {
                                                // Wyświetlenie błędu, jeśli zapis do Firestore nie powiódł się
                                                Toast.makeText(context, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            } else {
                                // Wyświetlenie błędu, jeśli rejestracja nie powiodła się
                                Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier
                .border(3.dp, Color.Black, MaterialTheme.shapes.extraLarge) // Obramowanie przycisku
                .clip(MaterialTheme.shapes.extraLarge), // Zaokrąglenie rogów przycisku
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Kolor tła przycisku
        ) {
            Text(text = "JOIN US!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 22.sp) // Tekst przycisku
        }
    }
}
