package com.example.hangman

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangman.ui.theme.HangmanTheme

// Return a random word for the game
fun randomWord(): String {
    val words = listOf("ANDROID", "KOTLIN", "COMPOSE", "DEVELOPER")
    return words.random()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangmanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HangmanGame(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HangmanGame(modifier: Modifier = Modifier) {
    // using rememberSaveable so that state survives rotations
    var currentWord by rememberSaveable { mutableStateOf(randomWord()) }
    var guessedLetters by rememberSaveable { mutableStateOf(setOf<Char>()) }
    var wrongGuesses by rememberSaveable { mutableStateOf(0) }
    var hintClickCount by rememberSaveable { mutableStateOf(0) }
    val maxWrong = 6

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Check for win/lose conditions
    val gameWon = currentWord.all { guessedLetters.contains(it) }
    val gameLost = wrongGuesses >= maxWrong

    if (isLandscape) {
        // Landscape layout: Three panels side by side
        Row(modifier = modifier.fillMaxSize().padding(8.dp)) {
            // Panel 1: Letter selection
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text("Choose a letter:")
                // wrap the letter grid in a Box
                Box(modifier = Modifier.weight(1f)) {
                    LetterButtons(
                        guessedLetters = guessedLetters,
                        onLetterClick = { letter ->
                            if (!currentWord.contains(letter)) wrongGuesses++
                            guessedLetters = guessedLetters + letter
                        }
                    )
                }
            }
            // Panel 2: Hint panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Hint Panel")
                Spacer(modifier = Modifier.height(8.dp))
                HintButton(
                    hintClickCount = hintClickCount,
                    onHintClick = {
                        if (wrongGuesses >= maxWrong - 1) {
                            Toast.makeText(context, "Hint not available", Toast.LENGTH_SHORT).show()
                        } else {
                            when (hintClickCount) {
                                0 -> {
                                    // First click: word length
                                    Toast.makeText(context, "Word length: ${currentWord.length}", Toast.LENGTH_SHORT).show()
                                    hintClickCount++
                                }
                                1 -> {
                                    // Second click: disables half of the remaining letters not in the word
                                    val remainingLetters = ('A'..'Z').filter { it !in guessedLetters && it !in currentWord }
                                    val toDisable = remainingLetters.shuffled().take(remainingLetters.size / 2)
                                    guessedLetters = guessedLetters + toDisable
                                    wrongGuesses++
                                    hintClickCount++
                                }
                                2 -> {
                                    // Third click: reveal all vowels (and disable them)
                                    val vowels = listOf('A', 'E', 'I', 'O', 'U')
                                    guessedLetters = guessedLetters + vowels
                                    wrongGuesses++
                                    hintClickCount++
                                }
                            }
                        }
                    }
                )
            }
            // Panel 3: Main game screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                HangmanDiagram(wrongGuesses, maxWrong)
                WordDisplay(word = currentWord, guessedLetters = guessedLetters)
                if (gameWon) {
                    Text("You won!", color = Color.Green)
                } else if (gameLost) {
                    Text("You lost! The word was: $currentWord", color = Color.Red)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    // Reset game state
                    currentWord = randomWord()
                    guessedLetters = emptySet()
                    wrongGuesses = 0
                    hintClickCount = 0
                }) {
                    Text("New Game")
                }
            }
        }
    } else {
        // Portrait layout: Vertical layout with no hint panel
        Column(
            modifier = modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HangmanDiagram(wrongGuesses, maxWrong)
            WordDisplay(word = currentWord, guessedLetters = guessedLetters)
            if (gameWon) {
                Text("You won!", color = Color.Green)
            } else if (gameLost) {
                Text("You lost! The word was: $currentWord", color = Color.Red)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Choose a letter:")
            // wrap letter buttons in a Box
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            ) {
                LetterButtons(
                    guessedLetters = guessedLetters,
                    onLetterClick = { letter ->
                        if (!currentWord.contains(letter)) wrongGuesses++
                        guessedLetters = guessedLetters + letter
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Reset game state for a new game
                currentWord = randomWord()
                guessedLetters = emptySet()
                wrongGuesses = 0
                hintClickCount = 0
            }) {
                Text("New Game")
            }
        }
    }
}

@Composable
fun LetterButtons(
    guessedLetters: Set<Char>,
    onLetterClick: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(('A'..'Z').toList()) { letter ->
            Button(
                onClick = { onLetterClick(letter) },
                enabled = !guessedLetters.contains(letter),
                modifier = Modifier
                    .padding(4.dp)
                    .size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray
                ),
                // Adding a border to debug the button boundaries.
                shape = MaterialTheme.shapes.small
            ) {
            }
            Text(text = letter.toString(), fontSize = 16.sp, color = Color.Black)
        }
    }
}

@Composable
fun HintButton(hintClickCount: Int, onHintClick: () -> Unit) {
    Button(onClick = onHintClick, modifier = Modifier.padding(8.dp)) {
        Text("Hint (${hintClickCount + 1})")
    }
}

@Composable
fun HangmanDiagram(wrongGuesses: Int, maxWrong: Int) {
    //draws the man
    Canvas(modifier = Modifier.size(200.dp)) {
        val strokeWidth = 4f
        // Base
        drawLine(
            color = Color.Black,
            start = Offset(x = size.width * 0.1f, y = size.height * 0.95f),
            end = Offset(x = size.width * 0.9f, y = size.height * 0.95f),
            strokeWidth = strokeWidth
        )
        // Vertical pole
        drawLine(
            color = Color.Black,
            start = Offset(x = size.width * 0.2f, y = size.height * 0.95f),
            end = Offset(x = size.width * 0.2f, y = size.height * 0.1f),
            strokeWidth = strokeWidth
        )
        // Horizontal beam
        drawLine(
            color = Color.Black,
            start = Offset(x = size.width * 0.2f, y = size.height * 0.1f),
            end = Offset(x = size.width * 0.6f, y = size.height * 0.1f),
            strokeWidth = strokeWidth
        )
        // Rope
        drawLine(
            color = Color.Black,
            start = Offset(x = size.width * 0.6f, y = size.height * 0.1f),
            end = Offset(x = size.width * 0.6f, y = size.height * 0.25f),
            strokeWidth = strokeWidth
        )

        // Draw stick figure parts based on wrongGuesses
        var partsDrawn = 0
        // 1. Head
        if (wrongGuesses > partsDrawn) {
            drawCircle(
                color = Color.Red,
                center = Offset(x = size.width * 0.6f, y = size.height * 0.35f),
                radius = 20f,
                style = Stroke(width = strokeWidth)
            )
            partsDrawn++
        }
        // 2. Body
        if (wrongGuesses > partsDrawn) {
            drawLine(
                color = Color.Red,
                start = Offset(x = size.width * 0.6f, y = size.height * 0.37f),
                end = Offset(x = size.width * 0.6f, y = size.height * 0.55f),
                strokeWidth = strokeWidth
            )
            partsDrawn++
        }
        // 3. Left Arm
        if (wrongGuesses > partsDrawn) {
            drawLine(
                color = Color.Red,
                start = Offset(x = size.width * 0.6f, y = size.height * 0.45f),
                end = Offset(x = size.width * 0.5f, y = size.height * 0.4f),
                strokeWidth = strokeWidth
            )
            partsDrawn++
        }
        // 4. Right Arm
        if (wrongGuesses > partsDrawn) {
            drawLine(
                color = Color.Red,
                start = Offset(x = size.width * 0.6f, y = size.height * 0.45f),
                end = Offset(x = size.width * 0.7f, y = size.height * 0.4f),
                strokeWidth = strokeWidth
            )
            partsDrawn++
        }
        // 5. Left Leg
        if (wrongGuesses > partsDrawn) {
            drawLine(
                color = Color.Red,
                start = Offset(x = size.width * 0.6f, y = size.height * 0.55f),
                end = Offset(x = size.width * 0.55f, y = size.height * 0.7f),
                strokeWidth = strokeWidth
            )
            partsDrawn++
        }
        // 6. Right Leg
        if (wrongGuesses > partsDrawn) {
            drawLine(
                color = Color.Red,
                start = Offset(x = size.width * 0.6f, y = size.height * 0.55f),
                end = Offset(x = size.width * 0.65f, y = size.height * 0.7f),
                strokeWidth = strokeWidth
            )

        }
    }
}

@Composable
fun WordDisplay(word: String, guessedLetters: Set<Char>) {
    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.Center) {
        for (letter in word) {
            val displayLetter = if (guessedLetters.contains(letter)) letter else '_'
            Text("$displayLetter ")
        }
    }
}

@Preview(showBackground = true, widthDp = 700, heightDp = 400)
@Composable
fun LandscapePreview() {
    HangmanTheme {
        HangmanGame()
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PortraitPreview() {
    HangmanTheme {
        HangmanGame()
    }
}
