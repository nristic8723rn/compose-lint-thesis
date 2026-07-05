package rs.diplomski.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Greeting()
            }
        }
    }
}

// NAMERNI PREKRŠAJ (Faza 1, korak 4): poziv zabranjeno() mora da
// izazove crvenu liniju u Android Studiju i pad komande lintDebug.
// Kad demo prođe (screenshot za rad!), zakomentarisati poziv.
@Composable
fun Greeting() {
    //zabranjeno()
    Text(text = "Zdravo, svete")
}

fun zabranjeno() {
    // Prazna funkcija - postoji samo da bi poziv mogao da se razreši.
}