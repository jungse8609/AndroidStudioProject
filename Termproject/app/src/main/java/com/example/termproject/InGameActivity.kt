package com.example.termproject

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

// Enum 클래스 정의
enum class DiceType(val value: Int) {
    ATTACK(0),
    DEFENSE(1),
    COUNTER(2);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value } ?: 0
    }
}

class InGameActivity : AppCompatActivity() {
    // XML Reference
    private lateinit var imgDiceAttack : ImageView
    private lateinit var imgDiceDefense : ImageView
    private lateinit var imgDiceCounter : ImageView

    private lateinit var imgHpBackground : ImageView
    private lateinit var imgCurrentHp : ImageView
    private lateinit var imgOpponentHpBackground : ImageView
    private lateinit var imgOpponentCurrentHp : ImageView

    private lateinit var imgOpponentDiceAttack : ImageView
    private lateinit var imgOpponentDiceDefense : ImageView
    private lateinit var imgOpponentDiceCounter : ImageView

    private lateinit var imgOpponentResult : ImageView
    private lateinit var imgPlayerResult : ImageView
    private lateinit var imgPlayerProfile : ImageView
    private lateinit var imgOpponentProfile : ImageView

    private lateinit var btnDice : Button
    private lateinit var btnAttack : Button
    private lateinit var btnDefense : Button
    private lateinit var btnCounter : Button
    private lateinit var btnGoLobby : Button

    private lateinit var txtOpponentResult : TextView
    private lateinit var txtPlayerResult : TextView
    private lateinit var txtRoundTimer: TextView
    private lateinit var txtHpBar : TextView
    private lateinit var txtOpponentHpBar : TextView
    private lateinit var txtResult : TextView
    private lateinit var txtScore : TextView
    private lateinit var txtPlayerType : TextView
    private lateinit var txtOpponentType : TextView
    private lateinit var txtPlayerNick : TextView
    private lateinit var txtOpponentNick : TextView

    private lateinit var layoutResult : LinearLayout

    private lateinit var db: FirebaseFirestore

    // User Id
    private lateinit var playerId : String
    private lateinit var opponentId : String
    private lateinit var roomName : String
    private lateinit var roundTimerId : String
    private lateinit var acceptId: String
    // User Profile Image
    private lateinit var playerProfile : String
    private lateinit var opponentProfile : String
    // User Status
    private var playerStatus : Long = 1
    private var opponentStatus : Long = 1
    // User Nickname and Score
    private lateinit var playerNick : String
    private lateinit var opponentNick : String
    private var playerScore : Long = 0
    private var opponentScore : Long = 0
    // dice roll result tuple(int, int, int)
    private var playerRolls: Triple<Long, Long, Long>? = null
    private var opponentRolls: Triple<Long, Long, Long>? = null
    private var playerType: DiceType = DiceType.ATTACK
    private var opponentType: DiceType = DiceType.ATTACK
    // Health
    private var playerHealth : Long = 20
    private var opponentHealth : Long = 20
    private var curPlayerHealth : Long = 20
    private var curOpponentHealth : Long = 20

    private var roundTimer: CountDownTimer? = null
    private val roundTimeLimit: Long = 10000 // 15 seconds

    private var resultTimer: CountDownTimer? = null
    private val resultTimeLimit: Long = 3500 // 3.5 seconds

    // Flow Control Boolean
    private var isWaiting: Boolean = false // 상대방이 고를 때까지 기다리는 중인가
    private var rollDiceOnce: Boolean = false // 라운드에 주사위는 한 번만 굴릴 수 있다

    private val imgNameAttack = listOf(R.drawable.one_red, R.drawable.two_red, R.drawable.three_red, R.drawable.four_red, R.drawable.five_red, R.drawable.six_red)
    private val imgNameDefense = listOf(R.drawable.one_blue, R.drawable.two_blue, R.drawable.three_blue, R.drawable.four_blue, R.drawable.five_blue, R.drawable.six_blue)
    private val imgNameCounter = listOf(R.drawable.one_purple, R.drawable.two_purple, R.drawable.three_purple, R.drawable.four_purple, R.drawable.five_purple, R.drawable.six_purple)

    // Constant
    private val colorTextDisable       : Int = Color.parseColor("#444444")
    private val colorTextAttackEnable  : Int = Color.parseColor("#b80080")
    private val colorTextDefenseEnable : Int = Color.parseColor("#00008b")
    private val colorTextCounterEnable : Int = Color.parseColor("#9011d3")
    private val colorTextDiceEnable : Int = Color.parseColor("#333333")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingame)

        // ### Init Variables
        imgDiceAttack  = findViewById(R.id.ImgDiceAttack)
        imgDiceDefense = findViewById(R.id.ImgDiceDefense)
        imgDiceCounter = findViewById(R.id.ImgDiceCounter)

        imgOpponentDiceAttack  = findViewById(R.id.ImgOpponentDiceAttack)
        imgOpponentDiceDefense = findViewById(R.id.ImgOpponentDiceDefense)
        imgOpponentDiceCounter = findViewById(R.id.ImgOpponentDiceCounter)

        imgOpponentResult = findViewById(R.id.ImgOpponentResult)
        imgPlayerResult = findViewById(R.id.ImgPlayerResult)

        imgHpBackground = findViewById(R.id.ImgHpBackround)
        imgCurrentHp = findViewById(R.id.ImgCurrentHp)
        imgOpponentHpBackground = findViewById(R.id.ImgOpponentHpBackround)
        imgOpponentCurrentHp = findViewById(R.id.ImgOpponentCurrentHp)

        imgPlayerProfile = findViewById(R.id.ImgPlayerProfile)
        imgOpponentProfile = findViewById(R.id.ImgOpponentProfile)

        btnDice = findViewById(R.id.BtnDiceRoll)
        btnAttack = findViewById(R.id.BtnAttack)
        btnDefense = findViewById(R.id.BtnDefense)
        btnCounter = findViewById(R.id.BtnCounter)
        btnGoLobby = findViewById(R.id.BtnGoLobby)

        txtRoundTimer = findViewById(R.id.TxtRoundTimer)
        txtPlayerResult = findViewById(R.id.TxtPlayerResult)
        txtOpponentResult = findViewById(R.id.TxtOpponentResult)
        txtPlayerType = findViewById(R.id.TxtPlayerType)
        txtOpponentType = findViewById(R.id.TxtOpponentType)
        txtHpBar = findViewById(R.id.TxtHpBar)
        txtOpponentHpBar = findViewById(R.id.TxtOpponentHpBar)
        txtResult = findViewById(R.id.TxtResult)
        txtScore = findViewById(R.id.TxtScore)
        txtPlayerNick = findViewById(R.id.TxtPlayerNick)
        txtOpponentNick = findViewById(R.id.TxtOpponentNick)

        layoutResult = findViewById(R.id.LayoutResult)

        // SFX - BGM
        SoundManager.playBackgroundMusic(SoundManager.Bgm.GAME)

        // Firebase 에서 read 해와야함 Hp Section
        db = FirebaseFirestore.getInstance()

        playerId = intent.getStringExtra("userId").toString()
        opponentId = intent.getStringExtra("opponentId").toString()
        roomName = intent.getStringExtra("roomName").toString()

        // Init Game Variables
        db.collection("BattleRooms")
            .document(roomName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    roundTimerId = document.getString("roundTimerId") ?: "null"
                    acceptId = document.getString("acceptId") ?: "null"

                    playerNick = document.getString(playerId + "Nick") ?: "null"
                    playerScore = document.getLong(playerId + "Score") ?: 0
                    playerHealth = document.getLong(playerId + "HP") ?: 0
                    playerStatus = document.getLong(playerId + "Status") ?: 0
                    playerProfile = document.getString(playerId + "Profile") ?: "null"

                    opponentNick = document.getString(opponentId + "Nick") ?: "null"
                    opponentScore = document.getLong(opponentId + "Score") ?: 0
                    opponentHealth = document.getLong(opponentId + "HP") ?: 0
                    opponentStatus = document.getLong(opponentId + "Status") ?: 0
                    opponentProfile = document.getString(opponentId + "Profile") ?: "null"

                    curPlayerHealth= playerHealth
                    curOpponentHealth = opponentHealth

                    // Update Hp Bar
                    imgHpBackground.post {
                        val backgroundWidth = imgHpBackground.width
                        val curWidth = (backgroundWidth * (curPlayerHealth.toFloat() / playerHealth)).toInt()
                        val layoutParams = imgCurrentHp.layoutParams
                        layoutParams.width = curWidth
                        imgCurrentHp.layoutParams = layoutParams
                    }

                    imgOpponentHpBackground.post {
                        val backgroundWidth = imgOpponentHpBackground.width
                        val curWidth = (backgroundWidth * (curOpponentHealth.toFloat() / opponentHealth)).toInt()
                        val layoutParams = imgOpponentCurrentHp.layoutParams
                        layoutParams.width = curWidth
                        imgOpponentCurrentHp.layoutParams = layoutParams
                    }
                    txtHpBar.text = "$curPlayerHealth/$playerHealth"
                    txtOpponentHpBar.text = "$curOpponentHealth/$opponentHealth"

                    txtPlayerType.text = ""
                    txtOpponentType.text = ""

                    txtPlayerNick.text = playerNick
                    txtOpponentNick.text = opponentNick

                    imgPlayerProfile.setImageResource(resources.getIdentifier(playerProfile, "drawable", packageName))
                    imgOpponentProfile.setImageResource(resources.getIdentifier(opponentProfile, "drawable", packageName))

                    // ### Round Play while someone died
                    gameplay();
                }
            }

        monitoringOpponentStatus()

        btnDice.setSoundEffectsEnabled(false)
        btnAttack.setSoundEffectsEnabled(false)
        btnDefense.setSoundEffectsEnabled(false)
        btnCounter.setSoundEffectsEnabled(false)
        btnGoLobby.setSoundEffectsEnabled(false)

        btnDice.setOnClickListener {
            if (!rollDiceOnce) {
                rollDiceOnce = true
                playerRolls = rollDices()

                // SFX
                SoundManager.playSoundEffect(R.raw.sfx_rolldice)

                // firebase에 주사위 값 write
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Attack", playerRolls!!.first)
                            document.reference.update(playerId + "Defense", playerRolls!!.second)
                            document.reference.update(playerId + "Counter", playerRolls!!.third)

                            // 버튼 interactive 설정
                            btnDice.isEnabled    = false; btnDice   .setTextColor(colorTextDisable);       btnDice.background = null
                            btnAttack.isEnabled  = true;  btnAttack .setTextColor(colorTextAttackEnable);  btnAttack .setBackgroundResource(R.drawable.button_yellow_border)
                            btnDefense.isEnabled = true;  btnDefense.setTextColor(colorTextDefenseEnable); btnDefense.setBackgroundResource(R.drawable.button_yellow_border)
                            btnCounter.isEnabled = true;  btnCounter.setTextColor(colorTextCounterEnable); btnCounter.setBackgroundResource(R.drawable.button_yellow_border)

                            onUpdateDiceImage()
                        }
                    }
            }
        }

        btnAttack.setOnClickListener {
            if (!isWaiting && rollDiceOnce) {
                playerType = DiceType.ATTACK

                // SFX
                SoundManager.playSoundEffect(R.raw.sfx_click02)

                // firebase에 주사위 type write
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Choose", 0)
                            document.reference.update(playerId + "Round", 1)

                            imgDiceAttack.setBackgroundResource(R.drawable.button_green_border)
                            btnAttack.setTextColor(colorTextAttackEnable); btnAttack .background = null
                            btnDefense.setTextColor(colorTextDisable); btnDefense.background = null
                            btnCounter.setTextColor(colorTextDisable); btnCounter.background = null
                            waitForOpponent()
                        }
                    }
            }
        }

        btnDefense.setOnClickListener {
            if (!isWaiting && rollDiceOnce) {
                playerType = DiceType.DEFENSE

                // SFX
                SoundManager.playSoundEffect(R.raw.sfx_click02)

                // firebase에 주사위 type write
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Choose", 1)
                            document.reference.update(playerId + "Round", 1)

                            imgDiceDefense.setBackgroundResource(R.drawable.button_green_border)
                            btnAttack.setTextColor(colorTextDisable); btnAttack.background = null
                            btnDefense.setTextColor(colorTextDefenseEnable); btnDefense.background = null
                            btnCounter.setTextColor(colorTextDisable); btnCounter.background = null
                            waitForOpponent()
                        }
                    }
            }
        }

        btnCounter.setOnClickListener {
            if (!isWaiting && rollDiceOnce) {
                playerType = DiceType.COUNTER

                // SFX
                SoundManager.playSoundEffect(R.raw.sfx_click02)

                // firebase에 주사위 type write
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Choose", 2)
                            document.reference.update(playerId + "Round", 1)

                            imgDiceCounter.setBackgroundResource(R.drawable.button_green_border)
                            btnAttack .setTextColor(colorTextDisable); btnAttack.background = null
                            btnDefense.setTextColor(colorTextDisable); btnDefense.background = null
                            btnCounter.setTextColor(colorTextCounterEnable); btnCounter.background = null
                            waitForOpponent()
                        }
                    }
            }
        }

        btnGoLobby.setOnClickListener {
            // SFX
            SoundManager.playSoundEffect(R.raw.sfx_click02)

            db.collection("BattleRooms").document(roomName).delete()
                .addOnSuccessListener {
                    db.collection("BattleWait").document(acceptId).delete()
                        .addOnSuccessListener {
                            runOnUiThread {
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread {
                                Toast.makeText(this@InGameActivity, "Error out of battle room: $e", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        Toast.makeText(this@InGameActivity, "Error out of battle room: $e", Toast.LENGTH_SHORT).show()
                    }
                }

            SoundManager.playBackgroundMusic(SoundManager.Bgm.LOBBY)

            finish()
        }
    }

    // 세 개의 주사위를 굴리고 결과를 Triple로 반환하는 함수입니다.
    private fun rollDices(): Triple<Long, Long, Long> {
        val attackRoll = rollDice()
        val defenseRoll = rollDice()
        val counterRoll = rollDice()

        return Triple(attackRoll, defenseRoll, counterRoll)
    }

    // 주사위를 굴리는 함수입니다
    private fun rollDice(): Long {
        return Random.nextLong(1, 7) // 1부터 6까지의 랜덤 숫자를 반환합니다
    }

    // 던진 주사위 결과에 따른 ImageView 의 이미지 업데이트
    private fun onUpdateDiceImage() {
        var attackValue = playerRolls?.first ?: 1
        var defenseValue = playerRolls?.second ?: 1
        var counterValue = playerRolls?.third ?: 1
        imgDiceAttack.setImageResource(imgNameAttack[attackValue.toInt() - 1])
        imgDiceDefense.setImageResource(imgNameDefense[defenseValue.toInt() - 1])
        imgDiceCounter.setImageResource(imgNameCounter[counterValue.toInt() - 1])
    }

    private fun gameplay() {
        // 만약 한쪽이 죽었으면 게임 종료
        if (curPlayerHealth <= 0 || curOpponentHealth <= 0) {
            exitGame()
            return
        }

        // Control 제어 State 들 초기화
        rollDiceOnce = false;
        isWaiting = false;

        // 버튼 interactive 설정
        btnDice.isEnabled = true;     btnDice   .setTextColor(colorTextDiceEnable); btnDice.setBackgroundResource(R.drawable.button_yellow_border)
        btnAttack.isEnabled = false;  btnAttack .setTextColor(colorTextDisable)  ;btnAttack.background = null
        btnDefense.isEnabled = false; btnDefense.setTextColor(colorTextDisable);btnDefense.background = null
        btnCounter.isEnabled = false; btnCounter.setTextColor(colorTextDisable);btnCounter.background = null

        // 상대방 주사위 이미지 안 보이게 설정
        imgOpponentDiceAttack.setImageResource(R.drawable.question_red)
        imgOpponentDiceDefense.setImageResource(R.drawable.question_blue)
        imgOpponentDiceCounter.setImageResource(R.drawable.question_purple)

        // 내 주사위 자리는 이미지를 제거하여 초기화
        imgDiceAttack.setImageResource(0)
        imgDiceDefense.setImageResource(0)
        imgDiceCounter.setImageResource(0)

        // 주사위의 테두리 검은색으로 초기화
        imgDiceAttack.setBackgroundResource(R.drawable.button_white_border)
        imgDiceDefense.setBackgroundResource(R.drawable.button_white_border)
        imgDiceCounter.setBackgroundResource(R.drawable.button_white_border)
        imgOpponentDiceAttack.setBackgroundResource(R.drawable.button_white_border)
        imgOpponentDiceDefense.setBackgroundResource(R.drawable.button_white_border)
        imgOpponentDiceCounter.setBackgroundResource(R.drawable.button_white_border)

        // 결과 텍스트, 이미지 초기화
        txtPlayerType.text = ""
        txtOpponentType.text = ""
        txtPlayerResult.text = "-"
        txtOpponentResult.text = "-"
        imgPlayerResult.setImageResource(0)
        imgOpponentResult.setImageResource(0)

        // 라운드 타이머 실행
        startRoundTimer()
    }

    private var listenerWaitOpponent: ListenerRegistration? = null

    // 상대방의 주사위 결과를 기다리는 함수
    private fun waitForOpponent() {
        isWaiting = true

        listenerWaitOpponent?.remove()
        listenerWaitOpponent = db.collection("BattleRooms").document(roomName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // 상대방의 round = true 인지 확인
                    val opponentRound = snapshot.getLong(opponentId + "Round") ?: 0L
                    val playerRound = snapshot.getLong(playerId + "Round") ?: 0L
                    if (opponentRound == 1L && playerRound == 1L) {
                        // 리스너 제거
                        listenerWaitOpponent?.remove()

                        var opponentAttack : Long = snapshot.getLong(opponentId + "Attack") ?: 1L
                        var opponentDefense : Long = snapshot.getLong(opponentId + "Defense") ?: 1L
                        var opponentCounter : Long = snapshot.getLong(opponentId + "Counter") ?: 1L
                        var opponentChoose = snapshot.getLong(opponentId + "Choose")

                        opponentRolls = Triple(opponentAttack, opponentDefense, opponentCounter)
                        when (opponentChoose) {
                            0L -> opponentType = DiceType.ATTACK
                            1L -> opponentType = DiceType.DEFENSE
                            2L -> opponentType = DiceType.COUNTER
                        }

                        // round timer 종료 후 게임 결과 프로세스로 넘어간다
                        roundTimer?.cancel()

                        showResult()
                    }
                }
            }
    }

    private lateinit var result : Pair<Long, Long>

    private fun showResult() {
        // Calculate Result
        result = getResult()

        // Set Player Result
        var attackValue = playerRolls?.first ?: 1
        var defenseValue = playerRolls?.second ?: 1
        var counterValue = playerRolls?.third ?: 1
        when (playerType) {
            DiceType.ATTACK -> imgDiceAttack.setImageResource(imgNameAttack[attackValue.toInt() - 1])
            DiceType.DEFENSE -> imgDiceDefense.setImageResource(imgNameDefense[defenseValue.toInt() - 1])
            DiceType.COUNTER -> imgDiceCounter.setImageResource(imgNameCounter[counterValue.toInt() - 1])
        }

        // Set Opponent Result
        var opAttackValue : Long = opponentRolls?.first ?: 1
        var opDefenseValue : Long = opponentRolls?.second ?: 1
        var opCounterValue : Long = opponentRolls?.third ?: 1
        imgOpponentDiceAttack.setImageResource(imgNameAttack[opAttackValue.toInt() - 1])
        imgOpponentDiceDefense.setImageResource(imgNameDefense[opDefenseValue.toInt() - 1])
        imgOpponentDiceCounter.setImageResource(imgNameCounter[opCounterValue.toInt() - 1])
        when (opponentType) {
            DiceType.ATTACK -> imgOpponentDiceAttack.setBackgroundResource(R.drawable.button_green_border)
            DiceType.DEFENSE -> imgOpponentDiceDefense.setBackgroundResource(R.drawable.button_green_border)
            DiceType.COUNTER -> imgOpponentDiceCounter.setBackgroundResource(R.drawable.button_green_border)
        }

        // Update Result UI
        when (playerType) {
            DiceType.ATTACK -> { imgPlayerResult.setImageResource(imgNameAttack[attackValue.toInt() - 1]); txtPlayerType.text = "공격" }
            DiceType.DEFENSE -> { imgPlayerResult.setImageResource(imgNameDefense[defenseValue.toInt() - 1]); txtPlayerType.text = "방어" }
            DiceType.COUNTER -> { imgPlayerResult.setImageResource(imgNameCounter[counterValue.toInt() - 1]); txtPlayerType.text = "카운터" }
        }

        when (opponentType) {
            DiceType.ATTACK -> { imgOpponentResult.setImageResource(imgNameAttack[opAttackValue.toInt() - 1]); txtOpponentType.text = "공격" }
            DiceType.DEFENSE -> { imgOpponentResult.setImageResource(imgNameDefense[opDefenseValue.toInt() - 1]); txtOpponentType.text = "방어" }
            DiceType.COUNTER -> { imgOpponentResult.setImageResource(imgNameCounter[opCounterValue.toInt() - 1]); txtOpponentType.text = "카운터" }
        }

        if (result.first >= 0)
            txtPlayerResult.text = "+" + result.first.toString()
        else
            txtPlayerResult.text = result.first.toString()

        if (result.second >= 0)
            txtOpponentResult.text = "+" + result.second.toString()
        else
            txtOpponentResult.text = result.second.toString()

        startResultTimer()
    }

    private fun getResult() : Pair<Long, Long> {
        var playerAttackValue : Long = playerRolls?.first ?: 0
        var playerDefenseValue : Long = playerRolls?.second ?: 0
        var playerCounterValue : Long = playerRolls?.third ?: 0
        val opponentAttackValue: Long = opponentRolls?.first ?: 0
        val opponentDefenseValue: Long = opponentRolls?.second ?: 0
        val opponentCounterValue: Long = opponentRolls?.third ?: 0

        var playerResult : Long = 0
        var opponentResult : Long = 0

        when {
            // 공격 vs 공격 : 둘 다 데미지 입음
            playerType == DiceType.ATTACK && opponentType == DiceType.ATTACK -> {
                playerResult = -opponentAttackValue
                opponentResult = -playerAttackValue
            }
            // 공격 vs 방어 : 공격이 크면 데미지, 방어가 크면 회복
            playerType == DiceType.ATTACK && opponentType == DiceType.DEFENSE -> {
                opponentResult = opponentDefenseValue - playerAttackValue
            }
            // 공격 vs 카운터
            playerType == DiceType.ATTACK && opponentType == DiceType.COUNTER -> {
                if (playerAttackValue > opponentCounterValue)
                    opponentResult = -(playerAttackValue + opponentCounterValue)
                else if (playerAttackValue < opponentCounterValue)
                    playerResult = -(playerAttackValue + opponentCounterValue)
                else
                    opponentResult = -playerAttackValue
            }
            // 방어 vs 공격 : 상대의 공격이 크면 데미지, 방어가 크면 회복
            playerType == DiceType.DEFENSE && opponentType == DiceType.ATTACK -> {
                playerResult = playerDefenseValue - opponentAttackValue
            }
            // 방어 vs 방어 : 아무 일도 없음
            playerType == DiceType.DEFENSE && opponentType == DiceType.DEFENSE -> {
                // 아무 일도 없음
            }
            // 방어 vs 카운터 : 방어가 회복
            playerType == DiceType.DEFENSE && opponentType == DiceType.COUNTER -> {
                playerResult = opponentCounterValue
            }
            // 카운터 vs 공격
            playerType == DiceType.COUNTER && opponentType == DiceType.ATTACK -> {
                if (opponentAttackValue > playerCounterValue)
                    playerResult = -(opponentAttackValue + playerCounterValue)
                else if (opponentAttackValue < playerCounterValue)
                    opponentResult = -(opponentAttackValue + playerCounterValue)
                else
                    playerResult = -opponentAttackValue
            }
            // 카운터 vs 방어 : 방어가 회복
            playerType == DiceType.COUNTER && opponentType == DiceType.DEFENSE -> {
                opponentResult = playerCounterValue
            }
            // 카운터 vs 카운터 : 더 큰 쪽이 데미지
            playerType == DiceType.COUNTER && opponentType == DiceType.COUNTER -> {
                if (playerCounterValue > opponentCounterValue)
                    opponentResult = -playerCounterValue
                else if (playerCounterValue < opponentCounterValue)
                    playerResult = -opponentCounterValue
            }
        }

        return Pair(playerResult, opponentResult)
    }

    fun Int.clamp(min: Int, max: Int): Int {
        return when {
            this < min -> min
            this > max -> max
            else -> this
        }
    }

    fun Long.clamp(min: Long, max: Long): Long {
        return when {
            this < min -> min
            this > max -> max
            else -> this
        }
    }

    // 다음 턴으로 진행하는 함수입니다.
    private fun proceedToNextTurn() {
        // 타이머 취소
        roundTimer?.cancel()
        resultTimer?.cancel()

        // 결과 계산
        curPlayerHealth += result.first
        curOpponentHealth += result.second
        curPlayerHealth = curPlayerHealth.clamp(0, playerHealth)
        curOpponentHealth = curOpponentHealth.clamp(0, opponentHealth)

        // firebase에 update 된 HP를 write
        db.collection("BattleRooms")
            .document(roomName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    document.reference.update(playerId + "HP", curPlayerHealth)
                    document.reference.update(opponentId + "HP", curOpponentHealth)

                    // Update Opponent Dice Image
                    var attackValue = opponentRolls?.first ?: 1
                    var defenseValue = opponentRolls?.second ?: 1
                    var counterValue = opponentRolls?.third ?: 1
                    imgOpponentDiceAttack.setImageResource(imgNameAttack[attackValue.toInt() - 1])
                    imgOpponentDiceDefense.setImageResource(imgNameDefense[defenseValue.toInt() - 1])
                    imgOpponentDiceCounter.setImageResource(imgNameCounter[counterValue.toInt() - 1])

                    // Update Player Hp Bar
                    imgHpBackground.post {
                        val backgroundWidth = imgHpBackground.width
                        val curWidth = (backgroundWidth * (curPlayerHealth.toFloat() / playerHealth)).toInt()
                        val layoutParams = imgCurrentHp.layoutParams
                        layoutParams.width = curWidth
                        imgCurrentHp.layoutParams = layoutParams
                    }
                    txtHpBar.text = "$curPlayerHealth/$playerHealth"

                    // Update Opponent Hp Bar
                    imgOpponentHpBackground.post {
                        val backgroundWidth = imgOpponentHpBackground.width
                        val curWidth = (backgroundWidth * (curOpponentHealth.toFloat() / opponentHealth)).toInt()
                        val layoutParams = imgOpponentCurrentHp.layoutParams
                        layoutParams.width = curWidth
                        imgOpponentCurrentHp.layoutParams = layoutParams
                    }
                    txtOpponentHpBar.text = "$curOpponentHealth/$opponentHealth"

                    // SFX
                    if (result.first > 0) // 화복
                        SoundManager.playSoundEffect(R.raw.sfx_heal02)
                    else if (result.first < 0) // 데미지
                        SoundManager.playSoundEffect(R.raw.sfx_damaged)
                    else if (result.second > 0) // 화복
                        SoundManager.playSoundEffect(R.raw.sfx_heal02)
                    else if (result.second < 0) // 데미지
                        SoundManager.playSoundEffect(R.raw.sfx_damaged)
                    else
                        SoundManager.playSoundEffect(R.raw.sfx_pass)

                    gameplay()
                }
            }
    }

    private var listenerResultRound: ListenerRegistration? = null

    // 결과창 타이머 시작 함수
    private fun startResultTimer() {
        lifecycleScope.launch {
            delay(resultTimeLimit)

            val document = db.collection("BattleRooms").document(roomName).get().await()

            document?.reference?.update(playerId + "Round", 0)
            document?.reference?.update(playerId + "Attack", 0)
            document?.reference?.update(playerId + "Defense", 0)
            document?.reference?.update(playerId + "Counter", 0)
            document?.reference?.update(playerId + "Choose", 0)

            listenerResultRound = db.collection("BattleRooms").document(roomName)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // 상대방의 round = true 인지 확인
                        val opponentRound = snapshot.getLong(opponentId + "Round") ?: 1L
                        val playerRound = snapshot.getLong(playerId + "Round") ?: 1L
                        if (opponentRound + playerRound == 0L) {
                            // 리스너 제거
                            listenerResultRound?.remove()

                            proceedToNextTurn()
                        }
                    }
                }
        }
    }

    private fun startRoundTimer() {
        lifecycleScope.launch {
            for (i in (roundTimeLimit / 1000) downTo 0) {
                txtRoundTimer.text = "$i"
                delay(1000)
            }

            onRoundTimerFinish()
        }
    }

    private suspend fun onRoundTimerFinish() {
        if (!isWaiting) {
            // Roll Dices Randomly
            playerRolls = rollDices()
            val randomChoose = Random.nextInt(0, 3) // return Random Integer between 0 and 2
            playerType = when (randomChoose) {
                0 -> DiceType.ATTACK
                1 -> DiceType.DEFENSE
                else -> DiceType.COUNTER
            }
            val document = db.collection("BattleRooms")
                .document(roomName)
                .get()
                .await()

            document?.reference?.update(playerId + "Attack", playerRolls!!.first)
            document?.reference?.update(playerId + "Defense", playerRolls!!.second)
            document?.reference?.update(playerId + "Counter", playerRolls!!.third)
            document?.reference?.update(playerId + "Choose", randomChoose)
            document?.reference?.update(playerId + "Round", 1)

            btnDice.setTextColor(colorTextDisable); btnDice.background = null
            btnAttack.setTextColor(colorTextDisable); btnAttack.background = null
            btnDefense.setTextColor(colorTextDisable); btnDefense.background = null
            btnCounter.setTextColor(colorTextDisable); btnCounter.background = null
            when (playerType) {
                DiceType.ATTACK -> { imgDiceAttack.setBackgroundResource(R.drawable.button_green_border); btnAttack.setTextColor(colorTextAttackEnable) }
                DiceType.DEFENSE -> { imgDiceDefense.setBackgroundResource(R.drawable.button_green_border); btnDefense.setTextColor(colorTextDefenseEnable) }
                DiceType.COUNTER -> { imgDiceCounter.setBackgroundResource(R.drawable.button_green_border); btnCounter.setTextColor(colorTextCounterEnable) }
            }
            onUpdateDiceImage()

            waitForOpponent()
        } else {
            waitForOpponent()
        }
    }

    private fun exitGame() {
        roundTimer?.cancel()
        resultTimer?.cancel()

        listenerWaitOpponent?.remove()
        listenerResultRound?.remove()

        var resultScore : Long

        // 무승부
        if (curPlayerHealth <= 0 && curOpponentHealth <= 0) {
            resultScore = playerScore
            showResultPopup("DRAW" , "$playerScore -> $resultScore", resultScore)
        }
        else if (curPlayerHealth <= 0) {
            resultScore = (playerScore - curOpponentHealth).clamp(0, Long.MAX_VALUE)
            showResultPopup("DEFEAT" , "$playerScore -> $resultScore", resultScore)
        }
        else {
            resultScore = (playerScore + curPlayerHealth).clamp(0, Long.MAX_VALUE)
            showResultPopup("WIN" , "$playerScore -> $resultScore", resultScore)
        }
    }

    private fun showResultPopup(result: String, scoreStr: String, score: Long) {
        // Set Result
        var playerScore : Long = 0

        // SFX
        SoundManager.playSoundEffect(R.raw.sfx_popup)

        db.collection("BattleRooms")
            .document(roomName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    playerScore = document.getLong(playerId + "Score") ?: 0
                }
            }

        txtResult.text = result
        txtScore.text = scoreStr

        db.collection("users")
            .document(playerId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    document.reference.update("Score", score)
                }
            }

        // Popup 창 띄우기
        layoutResult.post {
            val layoutParams = FrameLayout.LayoutParams(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, resources.displayMetrics).toInt(),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, resources.displayMetrics).toInt()
            )
            layoutResult.layoutParams.width = layoutParams.width
            layoutResult.layoutParams.height = layoutParams.height
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        db.collection("BattleRooms")
            .document(roomName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    document.reference.update(playerId + "Status", 0)
                }
            }
        finish()
    }

    private fun monitoringOpponentStatus() {
        // Firestore에서 사용자 상태를 실시간으로 모니터링
        db.collection("BattleRooms").document(roomName)
            .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("LogTemp", "Listen failed", e)
                return@addSnapshotListener
            }

                if (snapshot != null && snapshot.exists()) {
                    opponentStatus = snapshot.getLong(opponentId + "Status") ?: 0
                    if (opponentStatus == 0L) {
                        curOpponentHealth = 0
                        exitGame()
                    }
                }
        }
    }
}