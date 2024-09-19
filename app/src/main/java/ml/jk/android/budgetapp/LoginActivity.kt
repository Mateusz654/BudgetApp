package ml.jk.android.budgetapp

import android.content.Context
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
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

// Główna aktywność logowania
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge() // Umożliwia rozszerzenie widoków na krawędzie ekranu
        setContent {
            LoginView() // Ustawienie widoku logowania jako zawartości aktywności
        }
    }
}

// Funkcja Composable, która tworzy interfejs logowania
@Composable
fun LoginView() {
    val context = LocalContext.current // Pobranie kontekstu lokalnego
    var login by remember { mutableStateOf("") } // Stan przechowujący login
    var password by remember { mutableStateOf("") } // Stan przechowujący hasło
    var loginError by remember { mutableStateOf(false) } // Stan przechowujący informację o błędzie logowania

    // Kolumna zawierająca elementy logowania
    Column(
        modifier = Modifier
            .statusBarsPadding() // Dodaje odstęp na status bar
            .background(Color(0xffeb9234)) // Ustawienie tła
            .padding(10.dp) // Dodanie paddingu
            .fillMaxSize(), // Rozciągnięcie na pełny rozmiar
        verticalArrangement = Arrangement.Center, // Ustawienie pionowego rozmieszczenia
        horizontalAlignment = Alignment.CenterHorizontally // Ustawienie poziomego rozmieszczenia
    ) {
        // Tytuł aplikacji
        Text(
            text = "BUDGET APP",
            fontSize = 32.sp, // Rozmiar czcionki
            fontWeight = FontWeight.Bold, // Pogrubienie czcionki
            modifier = Modifier.padding(10.dp) // Padding
        )
        Spacer(modifier = Modifier.height(30.dp)) // Przestrzeń pomiędzy elementami
        // Pole tekstowe dla loginu
        TextField(
            modifier = Modifier.background(Color.White), // Tło pola tekstowego
            value = login,
            onValueChange = { login = it }, // Aktualizacja stanu loginu
            textStyle = TextStyle(fontSize = 20.sp), // Rozmiar czcionki w polu tekstowym
            label = { Text("Login or e-mail") } // Etykieta pola tekstowego
        )
        Spacer(modifier = Modifier.height(20.dp)) // Przestrzeń pomiędzy elementami
        // Pole tekstowe dla hasła
        TextField(
            modifier = Modifier.background(Color.White), // Tło pola tekstowego
            value = password,
            onValueChange = { password = it }, // Aktualizacja stanu hasła
            textStyle = TextStyle(fontSize = 20.sp), // Rozmiar czcionki w polu tekstowym
            label = { Text("Password") }, // Etykieta pola tekstowego
            visualTransformation = PasswordVisualTransformation() // Maskowanie hasła
        )
        Spacer(modifier = Modifier.height(40.dp)) // Przestrzeń pomiędzy elementami
        // Przycisk logowania
        Button(
            onClick = {
                if (login.contains("@")) {
                    // Logowanie przy użyciu e-maila
                    signInWithEmail(login, password, context) {
                        loginError = true // Ustawienie błędu logowania
                    }
                } else {
                    // Logowanie przy użyciu loginu
                    findEmailByLogin(login) { email ->
                        if (email != null) {
                            signInWithEmail(email, password, context) {
                                loginError = true // Ustawienie błędu logowania
                            }
                        } else {
                            loginError = true // Ustawienie błędu logowania, jeśli użytkownik nie został znaleziony
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier
                .border(3.dp, Color.Black, MaterialTheme.shapes.extraLarge) // Obrys przycisku
                .clip(MaterialTheme.shapes.extraLarge), // Zaokrąglenie rogów
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Kolor tła przycisku
        ) {
            Text(text = "LOG IN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 22.sp) // Tekst przycisku
        }
        Spacer(modifier = Modifier.height(30.dp)) // Przestrzeń pomiędzy elementami
        // Wyświetlanie komunikatu o błędzie logowania, jeśli wystąpił
        if (loginError) {
            Text(text = "Incorrect login or password. Try again.", color = Color.Red)
            loginError = false // Resetowanie stanu błędu
        }
        Spacer(modifier = Modifier.height(30.dp)) // Przestrzeń pomiędzy elementami
        // Przycisk rejestracji
        Button(
            onClick = {
                val intent = Intent(context, RegisterActivity::class.java)
                context.startActivity(intent) // Uruchomienie aktywności rejestracji
            },
            modifier = Modifier
                .border(3.dp, Color.Black, MaterialTheme.shapes.extraLarge) // Obrys przycisku
                .clip(MaterialTheme.shapes.extraLarge), // Zaokrąglenie rogów
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Kolor tła przycisku
        ) {
            Text(text = "REGISTER", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 22.sp) // Tekst przycisku
        }
    }
}

// Funkcja wyszukująca e-mail na podstawie loginu
private fun findEmailByLogin(login: String, onComplete: (String?) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .whereEqualTo("login", login) // Wyszukiwanie dokumentów z określonym loginem
        .limit(1) // Ograniczenie do jednego wyniku
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val email = documents.first().getString("email") // Pobranie e-maila z dokumentu
                onComplete(email) // Przekazanie e-maila do funkcji zwrotnej
            } else {
                onComplete(null) // Przekazanie null, jeśli użytkownik nie został znaleziony
            }
        }
        .addOnFailureListener {
            onComplete(null) // Przekazanie null w przypadku błędu
        }
}

// Funkcja logowania użytkownika przy użyciu e-maila i hasła
private fun signInWithEmail(email: String, password: String, context: Context, onError: () -> Unit) {
    Firebase.auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Logowanie się powiodło
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Ustawienie flag do czyszczenia stosu aktywności
                ContextCompat.startActivity(context, intent, null) // Uruchomienie aktywności głównej
            } else {
                // Logowanie się nie powiodło
                onError() // Wywołanie funkcji obsługującej błąd
                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show() // Wyświetlenie komunikatu o błędzie
            }
        }
}
