package com.example.flags

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.random.Random


class GameActivity : AppCompatActivity() {
    private lateinit var countriesList: List<Pair<String, String>>
    private lateinit var selectedCountry: String
    private lateinit var flagImageName: String
    private var currentQuestionIndex = 0
    private var score = 0
    private var highScore = 0
    private lateinit var vibrator: Vibrator
    private lateinit var timer: CountDownTimer
    private val TIMER_DURATION: Long = 15000
    private lateinit var timerTextView: TextView
    private var remainingTimeMillis: Long = TIMER_DURATION
    private var livesRemaining: Int = 10
    private lateinit var livesTextView: TextView
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer
    private var timerPaused = false
    private val handler = Handler()
    private var gameSummaryDialog: AlertDialog? = null
    private var startTimeMillis: Long = 0
    private var endTimeMillis: Long = 0
    private var totalElapsedTimeMillis: Long = 0
    private var totalQuestionsFaced: Int = 0
    private var totalCorrectAnswers: Int = 0
    private var beatHighScore : Boolean = false
    private var isSoundEnabled = true
    private var isHapticFeedbackEnabled = true
    private val shownFlags = mutableListOf<Int>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game) //load UI
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT //lock orientation to portrait to avoid distortion

        timerTextView = findViewById(R.id.timer_text_view)
        livesTextView = findViewById(R.id.lives_text_view)

        updateLivesText() //update number of lives left

        //declaration of correct and wrong sounds
        correctSound = MediaPlayer.create(this, R.raw.correct)
        wrongSound = MediaPlayer.create(this, R.raw.wrong)

        val sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE) //declare shared preferences
        highScore = sharedPreferences.getInt("HighScore", 0) //set high score to 0 at start
        isSoundEnabled = sharedPreferences.getBoolean("SoundEnabled", true)
        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)

        countriesList = readCountriesFromFile(this) //get countries list and image references from stored file
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator //initialise system vibrator

        startTimeMillis = System.currentTimeMillis() //record start time
        showNextQuestion() //call function
    }

    private fun startTimer() {
        timer = object : CountDownTimer(TIMER_DURATION, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                updateTimerText() //calculate time remaining and update timer
            }

            override fun onFinish() {
                val rootView = findViewById<View>(android.R.id.content)
                val snackbar = Snackbar.make(rootView, "Time's Up", Snackbar.LENGTH_LONG).apply {
                    setTextColor(ContextCompat.getColor(this@GameActivity, R.color.snackbarTextColor)) // Set text color
                    duration = Snackbar.LENGTH_LONG // Set duration
                    animationMode = Snackbar.ANIMATION_MODE_SLIDE // Set animation mode
                    setBackgroundTint(ContextCompat.getColor(this@GameActivity, R.color.wrong)) // Set background tint color
                    show()
                } // Show snackbar when time is up

                score -= 50 // Decrease score by 50 when time runs out
                val scoreTextView = findViewById<TextView>(R.id.score_text_view)
                scoreTextView.text = "$score" // Update score text view

                currentQuestionIndex++ // Increment number of flags displayed

                if (currentQuestionIndex < countriesList.size) {
                    showNextQuestion() // Call function to show next question
                } else {
                    val rootView = findViewById<View>(android.R.id.content)
                    val snackbar = Snackbar.make(rootView, "Out of questions", Snackbar.LENGTH_LONG).apply {
                        setTextColor(ContextCompat.getColor(this@GameActivity, R.color.black)) // Set text color
                        duration = Snackbar.LENGTH_LONG // Set duration
                        animationMode = Snackbar.ANIMATION_MODE_SLIDE // Set animation mode
                        setBackgroundTint(ContextCompat.getColor(this@GameActivity, R.color.yellow)) // Set background tint color
                        show()
                    }                }
            }

            private fun updateTimerText() {
                val seconds = (remainingTimeMillis / 1000).toInt() //calculate time left
                timerTextView.text = "Time Left: $seconds seconds" //show time left
            }
        }
        timer.start() //start timer
    }

    private fun showNextQuestion() {
        if (::timer.isInitialized) {
            timer.cancel()
        }

        if (gameSummaryDialog?.isShowing == true) {
            return //when the game is over and the summary is shown, the game will not continue in the background
        }

        startTimer() //start timer

        var randomCountryIndex = getRandomCountryIndex()
        while (randomCountryIndex in shownFlags) {
            randomCountryIndex = getRandomCountryIndex() // Get a new random index if it's already shown
        }

        val random = Random.Default
        val question = countriesList[randomCountryIndex]
        selectedCountry = question.first //get country from it
        flagImageName = question.second  //get flag index from it

        val flagImageView = findViewById<ImageView>(R.id.flagImageView)
        val flagResourceId = resources.getIdentifier(flagImageName, "drawable", packageName) //get flag image from drawable

        shownFlags.add(randomCountryIndex)

        if (flagResourceId != 0) {
            flagImageView.setImageResource(flagResourceId) //set image to be displayed
        } else {
            Log.e("GameActivity", "Flag Resource ID is 0 (not found)") //report to developer when flag is not found
        }

        val options = countriesList.map { it.first }
        val shuffledOptions = options.shuffled().take(4).toMutableList()

        val correctAnswer = selectedCountry

        //initialise buttons
        val option1btn = findViewById<Button>(R.id.option1Button)
        val option2btn = findViewById<Button>(R.id.option2Button)
        val option3btn = findViewById<Button>(R.id.option3Button)
        val option4btn = findViewById<Button>(R.id.option4Button)

        val correctAnswerIndex = random.nextInt(shuffledOptions.size)
        shuffledOptions[correctAnswerIndex] = correctAnswer //assign correct answer to variables

        while (shuffledOptions.size > 4) {
            shuffledOptions.removeAt(random.nextInt(shuffledOptions.size)) //remove additional options
        }

        //set text in buttons to options got from shuffling
        option1btn.text = shuffledOptions[0]
        option2btn.text = shuffledOptions[1]
        option3btn.text = shuffledOptions[2]
        option4btn.text = shuffledOptions[3]

        //check answer compared to correct answer
        option1btn.setOnClickListener { checkAnswer(option1btn, correctAnswer) }
        option2btn.setOnClickListener { checkAnswer(option2btn, correctAnswer) }
        option3btn.setOnClickListener { checkAnswer(option3btn, correctAnswer) }
        option4btn.setOnClickListener { checkAnswer(option4btn, correctAnswer) }

        val scoreTextView = findViewById<TextView>(R.id.score_text_view)
        scoreTextView.text = "$score" //set current score

        val bestTextView = findViewById<TextView>(R.id.best_text_view)
        bestTextView.text = "$highScore" //set high score

        timerTextView.visibility = View.VISIBLE //show timer
        updateTimerText() //update timer remaining time
    }

    private fun updateTimerText() {
        val seconds = (remainingTimeMillis / 1000).toInt() //calculate timer remaining time
        timerTextView.text = "Time Left: $seconds seconds" //display time remaining
    }

    //to check answer given
    private fun checkAnswer(clickedButton: Button, correctAnswer: String) {
        val userAnswer = clickedButton.text.toString() //convert text in clicked button to string
        if (userAnswer == correctAnswer) {
            val timeBonus = (remainingTimeMillis / 150).toInt() // Calculate time bonus based on remaining time
            score += timeBonus
            val rootView = findViewById<View>(android.R.id.content)
            val snackbar = Snackbar.make(rootView, "Correct Answer", Snackbar.LENGTH_LONG).apply {
                setTextColor(ContextCompat.getColor(this@GameActivity, R.color.snackbarTextColor)) // Set text color
                duration = Snackbar.LENGTH_LONG // Set duration
                animationMode = Snackbar.ANIMATION_MODE_SLIDE // Set animation mode
                setBackgroundTint(ContextCompat.getColor(this@GameActivity, R.color.right)) // Set background tint color
                show()
            }

            playCorrectSound() //play correct sound effect for correct ans
            vibrate(100) //give short vibration for correct ans
            clickedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.right)) //turn button green
            pauseTimer() // pause timer
            totalCorrectAnswers++ //increment correct answer count
            handler.postDelayed({
                Log.d("GameActivity", "Delayed action: Showing next question.")
                showNextQuestion() //show next question
            }, 2000) // wait for 2 seconds before showing the next question

        } else {
            livesRemaining-- //decrement lives remaining for wrong answer
            updateLivesText() //update no of lives displayed
            val rootView = findViewById<View>(android.R.id.content)
            val snackbar = Snackbar.make(rootView, "Wrong Answer", Snackbar.LENGTH_LONG).apply {
                setTextColor(ContextCompat.getColor(this@GameActivity, R.color.snackbarTextColor)) // Set text color
                duration = Snackbar.LENGTH_LONG // Set duration
                animationMode = Snackbar.ANIMATION_MODE_SLIDE // Set animation mode
                setBackgroundTint(ContextCompat.getColor(this@GameActivity, R.color.wrong)) // Set background tint color
                show()
            } //show snackbar
            playWrongSound() //play wrong sound effect
            vibrate(300) //give longer vibration
            clickedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.wrong)) //turn button red for wrong ans

            handler.postDelayed({
                Log.d("GameActivity", "Delayed action: Showing next question.")
                showNextQuestion() //show next question
            }, 1000) // Wait for 2 seconds before showing the next question

            if (livesRemaining <= 0) {
                val rootView = findViewById<View>(android.R.id.content)
                Snackbar.make(rootView, "Game Over! Out of Lives", Snackbar.LENGTH_LONG).show() //when out of lives, show that
                pauseTimer() //pause timer when game over
                showGameSummary() //display game summary when game over
            }
        }

        totalQuestionsFaced++ //increment no of questions faced for each question

        if (score > highScore) {
            highScore = score //equal score and high score when high score is beaten
            val sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt("HighScore", highScore) //store high score in shared preferences
            editor.apply()
            beatHighScore = true //invert boolean value for future reference
        }

        resetButtonColors() //reset button colors afterwards
    }

    private fun getRandomCountryIndex(): Int {
        val random = Random.Default
        return random.nextInt(countriesList.size) // Get a random index within the countriesList size
    }

    private fun readCountriesFromFile(context: Context): List<Pair<String, String>> { //function to read countries from file
        val countriesList = mutableListOf<Pair<String, String>>() //create mutable list of string, string pairs
        try {
            val inputStream = context.resources.openRawResource(R.raw.countries) //read the contents of index file stored in raw
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? //to get line as string
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",") //split the string by the comma in the middle
                if (parts.size == 2) {
                    val countryName = parts[0].trim() //first string is country name
                    val flagImageName = parts[1].trim() //second string is flag image index name
                    countriesList.add(Pair(countryName, flagImageName)) //add them to list
                }
            }
            reader.close() //close bufferedReader
        } catch (e: IOException) {
            e.printStackTrace() //error reporting
        }
        return countriesList //return to main method
    }

    private fun updateLivesText() {
        livesTextView.text = "Lives Remaining: $livesRemaining" //update text of lives remaining
    }

    private fun moveToHomeActivity() { //move to home activity after finishing game activity
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun vibrate(duration: Long) { //vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isHapticFeedbackEnabled) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun resetButtonColors() { //reset button colors to default at the end of each question
        val option1btn = findViewById<Button>(R.id.option1Button)
        val option2btn = findViewById<Button>(R.id.option2Button)
        val option3btn = findViewById<Button>(R.id.option3Button)
        val option4btn = findViewById<Button>(R.id.option4Button)

        option1btn.setBackgroundColor(ContextCompat.getColor(this, R.color.default_btn_col))
        option2btn.setBackgroundColor(ContextCompat.getColor(this, R.color.default_btn_col))
        option3btn.setBackgroundColor(ContextCompat.getColor(this, R.color.default_btn_col))
        option4btn.setBackgroundColor(ContextCompat.getColor(this, R.color.default_btn_col))
    }

    private fun disableOptionButtons() { //disable buttons to be clicked
        val option1btn = findViewById<Button>(R.id.option1Button)
        val option2btn = findViewById<Button>(R.id.option2Button)
        val option3btn = findViewById<Button>(R.id.option3Button)
        val option4btn = findViewById<Button>(R.id.option4Button)

        option1btn.isEnabled = false
        option2btn.isEnabled = false
        option3btn.isEnabled = false
        option4btn.isEnabled = false
    }

    private fun enableOptionButtons() { //enable buttons to be clicked
        val option1btn = findViewById<Button>(R.id.option1Button)
        val option2btn = findViewById<Button>(R.id.option2Button)
        val option3btn = findViewById<Button>(R.id.option3Button)
        val option4btn = findViewById<Button>(R.id.option4Button)

        option1btn.isEnabled = true
        option2btn.isEnabled = true
        option3btn.isEnabled = true
        option4btn.isEnabled = true
    }

    override fun onDestroy() { //release resources upon destruction
        super.onDestroy()
        correctSound.release()
        wrongSound.release()
        gameSummaryDialog?.dismiss()
    }

    private fun pauseTimer() {
        timer.cancel() //pause timer
        timerPaused = true
    }

    private fun resumeTimer() {
        if (timerPaused) {
            startTimer() //resume timer
            timerPaused = false
        }
    }

    private fun playCorrectSound() {
        if (isSoundEnabled && !correctSound.isPlaying) { // Check sound setting
            correctSound.start()
        }
    }

    private fun playWrongSound() {
        if (isSoundEnabled && !wrongSound.isPlaying) { // Check sound setting
            wrongSound.start()
        }
    }

    private fun showGameSummary() { //function to display game summary at the end
        pauseTimer()
        val dialogView = layoutInflater.inflate(R.layout.game_summary_dialog, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val titleTextView = dialogView.findViewById<TextView>(R.id.title_text_view)
        val summaryTextView = dialogView.findViewById<TextView>(R.id.summary_text_view)
        val closeButton = dialogView.findViewById<Button>(R.id.close_button)

        val flagsFaced = currentQuestionIndex + 1
        val correct = score
        endTimeMillis = System.currentTimeMillis() //get end time
        totalElapsedTimeMillis = endTimeMillis - startTimeMillis //calculate time taken
        val timeTaken = totalElapsedTimeMillis / 1000 //convert to seconds

        val summaryText = "Total Questions Faced: $totalQuestionsFaced\n" +
                "Total Correct Answers: $totalCorrectAnswers\n" +
                "Total Time Taken: $timeTaken seconds\n" +
                "Score: $score\n" +
                if (beatHighScore) "Congratulations! You beat the high score!" else ""

        summaryTextView.text = summaryText //summary text passed to TextView

        gameSummaryDialog = builder.create()
        gameSummaryDialog?.show() //Show summary dialog

        closeButton.setOnClickListener {
            gameSummaryDialog?.dismiss() //dismiss when close button clicked
            moveToHomeActivity() //move to home afterwards
        }
    }
}