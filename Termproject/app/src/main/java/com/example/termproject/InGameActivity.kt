package com.example.termproject

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private lateinit var imgDiceAttackBackground : ImageView
    private lateinit var imgDiceDefenseBackground : ImageView
    private lateinit var imgDiceCounterBackground : ImageView

    private lateinit var imgHpBackground : ImageView
    private lateinit var imgCurrentHp : ImageView
    private lateinit var imgOpponentHpBackground : ImageView
    private lateinit var imgOpponentCurrentHp : ImageView

    private lateinit var imgOpponentDiceAttack : ImageView
    private lateinit var imgOpponentDiceDefense : ImageView
    private lateinit var imgOpponentDiceCounter : ImageView
    private lateinit var imgOpponentDiceAttackBackground : ImageView
    private lateinit var imgOpponentDiceDefenseBackground : ImageView
    private lateinit var imgOpponentDiceCounterBackground : ImageView

    private lateinit var imgOppoenentResult : ImageView
    private lateinit var imgPlayerResult : ImageView

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

    private lateinit var layoutResult : LinearLayout

    private lateinit var db: FirebaseFirestore

    // User Id
    private lateinit var playerId : String
    private lateinit var opponentId : String
    private lateinit var roomName : String
    private lateinit var roundTimerId : String
    private lateinit var acceptId: String
    // User Nickname and Score
    private lateinit var playerNick : String
    private lateinit var OpponentNick : String
    private var playerScore : Long = 0
    private var opponentScore : Long = 0
    // dice roll result tuple(int, int, int)
    private var playerRolls: Triple<Int, Int, Int>? = null
    private var opponentRolls: Triple<Int, Int, Int>? = null
    private var playerType: DiceType = DiceType.ATTACK
    private var opponentType: DiceType = DiceType.ATTACK
    // Health
    private var playerHealth : Long = 20
    private var opponentHealth : Long = 20
    private var curPlayerHealth : Long = 20
    private var curOpponentHealth : Long = 20

    private var roundTimer: CountDownTimer? = null
    private val roundTimeLimit: Long = 15000 // 15 seconds

    private var resultTimer: CountDownTimer? = null
    private val resultTimeLimit: Long = 3500 //3500 // 3.5 seconds

    // Flow Control Boolean
    private var isWaiting: Boolean = false // 상대방이 고를 때까지 기다리는 중인가
    private var rollDiceOnce: Boolean = false // 라운드에 주사위는 한 번만 굴릴 수 있다

    private val imgName = listOf(R.drawable.one, R.drawable.two, R.drawable.three, R.drawable.four, R.drawable.five, R.drawable.six)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingame)

        // ### Init Variables
        imgDiceAttack  = findViewById(R.id.ImgDiceAttack)
        imgDiceDefense = findViewById(R.id.ImgDiceDefense)
        imgDiceCounter = findViewById(R.id.ImgDiceCounter)
        imgDiceAttackBackground  = findViewById(R.id.ImgDiceAttackBackground)
        imgDiceDefenseBackground = findViewById(R.id.ImgDiceDefenseBackground)
        imgDiceCounterBackground = findViewById(R.id.ImgDiceCounterBackground)

        imgOpponentDiceAttack  = findViewById(R.id.ImgOpponentDiceAttack)
        imgOpponentDiceDefense = findViewById(R.id.ImgOpponentDiceDefense)
        imgOpponentDiceCounter = findViewById(R.id.ImgOpponentDiceCounter)
        imgOpponentDiceAttackBackground  = findViewById(R.id.ImgOpponentDiceAttackBackground)
        imgOpponentDiceDefenseBackground = findViewById(R.id.ImgOpponentDiceDefenseBackground)
        imgOpponentDiceCounterBackground = findViewById(R.id.ImgOpponentDiceCounterBackground)

        imgOppoenentResult = findViewById(R.id.ImgOpponentResult)
        imgPlayerResult = findViewById(R.id.ImgPlayerResult)

        imgHpBackground = findViewById(R.id.ImgHpBackround)
        imgCurrentHp = findViewById(R.id.ImgCurrentHp)
        imgOpponentHpBackground = findViewById(R.id.ImgOpponentHpBackround)
        imgOpponentCurrentHp = findViewById(R.id.ImgOpponentCurrentHp)

        btnDice = findViewById(R.id.BtnDiceRoll)
        btnAttack = findViewById(R.id.BtnAttack)
        btnDefense = findViewById(R.id.BtnDefense)
        btnCounter = findViewById(R.id.BtnCounter)
        btnGoLobby = findViewById(R.id.BtnGoLobby)

        txtRoundTimer = findViewById(R.id.TxtRoundTimer)
        txtPlayerResult = findViewById(R.id.TxtPlayerResult)
        txtOpponentResult = findViewById(R.id.TxtOpponentResult)
        txtHpBar = findViewById(R.id.TxtHpBar)
        txtOpponentHpBar = findViewById(R.id.TxtOpponentHpBar)
        txtResult = findViewById(R.id.TxtResult)
        txtScore = findViewById(R.id.TxtScore)

        layoutResult = findViewById(R.id.LayoutResult)

        // Firebase 에서 read 해와야함 Hp Section
        db = FirebaseFirestore.getInstance()

        playerId = intent.getStringExtra("userId").toString()
        opponentId = intent.getStringExtra("opponentId").toString()
        roomName = intent.getStringExtra("roomName").toString()

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

                    OpponentNick = document.getString(opponentId + "Nick") ?: "null"
                    opponentScore = document.getLong(opponentId + "Score") ?: 0
                    opponentHealth = document.getLong(opponentId + "HP") ?: 0

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

                    // ### Round Play while someone died
                    gameplay();
                }
            }

        btnDice.setOnClickListener {
            if (!rollDiceOnce) {
                rollDiceOnce = true
                playerRolls = rollDices()

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
                            btnDice.isEnabled = false; btnDice.setTextColor(Color.parseColor("#dddddd"))
                            btnAttack.isEnabled = true; btnAttack.setTextColor(Color.parseColor("#b80080"))
                            btnDefense.isEnabled = true; btnDefense.setTextColor(Color.parseColor("#00008b"))
                            btnCounter.isEnabled = true; btnCounter.setTextColor(Color.parseColor("#9011d3"))

                            onUpdateDiceImage()
                        }
                    }
            }
        }

        btnAttack.setOnClickListener {
            if (!isWaiting && rollDiceOnce) {
                playerType = DiceType.ATTACK

                // firebase에 주사위 type write
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Choose", 0)
                            document.reference.update(playerId + "Round", 1)

                            imgDiceAttackBackground.setBackgroundColor(Color.GREEN)
                            waitForOppoenent()
                        }
                    }
            }
        }

        btnDefense.setOnClickListener {
            if (!isWaiting && rollDiceOnce) {
                playerType = DiceType.DEFENSE

                // firebase에 주사위 type write
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Choose", 1)
                            document.reference.update(playerId + "Round", 1)

                            imgDiceDefenseBackground.setBackgroundColor(Color.GREEN)
                            waitForOppoenent()
                        }
                    }
            }
        }

        btnCounter.setOnClickListener {
            if (!isWaiting && rollDiceOnce) {
                playerType = DiceType.COUNTER

                // firebase에 주사위 type write
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Choose", 2)
                            document.reference.update(playerId + "Round", 1)

                            imgDiceCounterBackground.setBackgroundColor(Color.GREEN)
                            waitForOppoenent()
                        }
                    }
            }
        }

        btnGoLobby.setOnClickListener {
            finish()
        }
    }

    // 세 개의 주사위를 굴리고 결과를 Triple로 반환하는 함수입니다.
    private fun rollDices(): Triple<Int, Int, Int> {
        val attackRoll = rollDice()
        val defenseRoll = rollDice()
        val counterRoll = rollDice()

        return Triple(attackRoll, defenseRoll, counterRoll)
    }

    // 주사위를 굴리는 함수입니다
    private fun rollDice(): Int {
        return Random.nextInt(1, 7) // 1부터 6까지의 랜덤 숫자를 반환합니다
    }

    // 던진 주사위 결과에 따른 ImageView 의 이미지 업데이트
    private fun onUpdateDiceImage() {
        var attackValue = playerRolls?.first ?: 1
        var defenseValue = playerRolls?.second ?: 1
        var counterValue = playerRolls?.third ?: 1
        imgDiceAttack.setImageResource(imgName[attackValue - 1])
        imgDiceDefense.setImageResource(imgName[defenseValue - 1])
        imgDiceCounter.setImageResource(imgName[counterValue - 1])
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
        btnDice.isEnabled = true; btnDice.setTextColor(Color.parseColor("#333333"))
        btnAttack.isEnabled = false; btnAttack.setTextColor(Color.parseColor("#dddddd"))
        btnDefense.isEnabled = false; btnDefense.setTextColor(Color.parseColor("#dddddd"))
        btnCounter.isEnabled = false; btnCounter.setTextColor(Color.parseColor("#dddddd"))

        // 상대방 주사위 이미지 안 보이게 설정
        imgOpponentDiceAttack.setImageResource(R.drawable.hide)
        imgOpponentDiceDefense.setImageResource(R.drawable.hide)
        imgOpponentDiceCounter.setImageResource(R.drawable.hide)

        // 주사위 뒷 이미지 안 보이게
        imgDiceAttackBackground.setBackgroundColor(Color.BLACK)
        imgDiceDefenseBackground.setBackgroundColor(Color.BLACK)
        imgDiceCounterBackground.setBackgroundColor(Color.BLACK)
        imgOpponentDiceAttackBackground.setBackgroundColor(Color.BLACK)
        imgOpponentDiceDefenseBackground.setBackgroundColor(Color.BLACK)
        imgOpponentDiceCounterBackground.setBackgroundColor(Color.BLACK)

        // 결과 텍스트 초기화
        imgPlayerResult.setImageResource(R.drawable.hide)
        imgOppoenentResult.setImageResource(R.drawable.hide)
        txtPlayerResult.text = "-"
        txtOpponentResult.text = "-"

        // 라운드 타이머 실행
        startRoundTimer()
    }

    private var listenerRegistration: ListenerRegistration? = null

    // 상대방의 주사위 결과를 기다리는 함수 (여기서는 단순히 랜덤 값을 설정합니다)
    private fun waitForOppoenent() {
        isWaiting = true

        // 간단한 구현으로, 실제 게임에서는 네트워크 대기 로직이 들어가야 함
        var opponentAttack : Int = 0
        var opponentDefense : Int = 0
        var opponentCounter : Int = 0
        var opponentChoose : Int = 0

        var flag = false

        listenerRegistration = db.collection("BattleRooms").document(roomName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error waiting for opponent acceptance", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists() && !flag) {
                    // 상대방의 선택했는지 = true 인지 확인
                    val opponentRound = snapshot.getLong(opponentId + "Round") ?: 0L
                    val playerRound = snapshot.getLong(playerId + "Round") ?: 0L
                    if (opponentRound == 1L && playerRound == 1L) {
                        flag = true

                        opponentAttack = snapshot.getLong(opponentId + "Attack")!!.toInt()
                        opponentDefense = snapshot.getLong(opponentId + "Defense")!!.toInt()
                        opponentCounter = snapshot.getLong(opponentId + "Counter")!!.toInt()
                        opponentChoose = snapshot.getLong(opponentId + "Choose")!!.toInt()
                        opponentRolls = Triple(opponentAttack, opponentDefense, opponentCounter)
                        when (opponentChoose) {
                            0 -> opponentType = DiceType.ATTACK
                            1 -> opponentType = DiceType.DEFENSE
                            2 -> opponentType = DiceType.COUNTER
                        }

                        // round timer 종료 후 게임 결과 프로세스로 넘어간다
                        roundTimer?.cancel()
                        showResult()

                        // 리스너 제거
                        listenerRegistration?.remove()
                    }
                }
            }
    }

    private lateinit var result : Pair<Int, Int>

    private fun showResult() {
        // 여기서 상대방의 선택이 끝나거나 타이머가 종료할 때까지 기다려야해
        // Calculate Result
        result = getResult()

        // Set Player Result
        var attackValue = playerRolls?.first ?: 1
        var defenseValue = playerRolls?.second ?: 1
        var counterValue = playerRolls?.third ?: 1
        when (playerType) {
            DiceType.ATTACK -> imgDiceAttack.setImageResource(imgName[attackValue - 1])
            DiceType.DEFENSE -> imgDiceDefense.setImageResource(imgName[defenseValue - 1])
            DiceType.COUNTER -> imgDiceCounter.setImageResource(imgName[counterValue - 1])
        }

        // Set Opponent Result
        var opAttackValue = opponentRolls?.first ?: 1
        var opDefenseValue = opponentRolls?.second ?: 1
        var opCounterValue = opponentRolls?.third ?: 1
        imgOpponentDiceAttack.setImageResource(imgName[opAttackValue - 1])
        imgOpponentDiceDefense.setImageResource(imgName[opDefenseValue - 1])
        imgOpponentDiceCounter.setImageResource(imgName[opCounterValue - 1])
        when (opponentType) {
            DiceType.ATTACK -> imgOpponentDiceAttackBackground.setBackgroundColor(Color.RED)
            DiceType.DEFENSE -> imgOpponentDiceDefenseBackground.setBackgroundColor(Color.RED)
            DiceType.COUNTER -> imgOpponentDiceCounterBackground.setBackgroundColor(Color.RED)
        }

        // Update Result
        when (playerType) {
            DiceType.ATTACK -> imgPlayerResult.setImageResource(imgName[attackValue - 1])
            DiceType.DEFENSE -> imgPlayerResult.setImageResource(imgName[defenseValue - 1])
            DiceType.COUNTER -> imgPlayerResult.setImageResource(imgName[counterValue - 1])
        }

        when (opponentType) {
            DiceType.ATTACK -> imgOppoenentResult.setImageResource(imgName[opAttackValue - 1])
            DiceType.DEFENSE -> imgOppoenentResult.setImageResource(imgName[opDefenseValue - 1])
            DiceType.COUNTER -> imgOppoenentResult.setImageResource(imgName[opCounterValue - 1])
        }

        if (result.first >= 0)
            txtPlayerResult.text = "+" + result.first.toString()
        else
            txtPlayerResult.text = result.first.toString()

        if (result.second >= 0)
            txtOpponentResult.text = "+" + result.second.toString()
        else
            txtOpponentResult.text = result.second.toString()



        // Finished() -> proceedToNextTurn
        startResultTimer()
    }

    private fun getResult() : Pair<Int, Int> {
        var playerAttackValue : Int = playerRolls?.first ?: 0
        var playerDefenseValue : Int = playerRolls?.second ?: 0
        var playerCounterValue : Int = playerRolls?.third ?: 0
        val opponentAttackValue: Int = opponentRolls?.first ?: 0
        val opponentDefenseValue: Int = opponentRolls?.second ?: 0
        val opponentCounterValue: Int = opponentRolls?.third ?: 0

        var playerResult : Int = 0
        var opponentResult : Int = 0

        when {
            // 공격 vs 공격 : 둘 다 데미지 입음
            playerType == DiceType.ATTACK && opponentType == DiceType.ATTACK -> {
                playerResult = opponentAttackValue * (-1)
                opponentResult = playerAttackValue * (-1)
            }
            // 공격 vs 방어 : 공격이 크면 데미지, 방어가 크면 회복
            playerType == DiceType.ATTACK && opponentType == DiceType.DEFENSE -> {
                if (playerAttackValue > opponentDefenseValue)
                    opponentResult = (playerAttackValue - opponentDefenseValue) * (-1)
                else
                    opponentResult = opponentDefenseValue - playerAttackValue
            }
            // 공격 vs 카운터
            playerType == DiceType.ATTACK && opponentType == DiceType.COUNTER -> {
                if (playerAttackValue > opponentCounterValue)
                    opponentResult = (playerAttackValue + opponentCounterValue) * (-1)
                else if (playerAttackValue < opponentCounterValue)
                    playerResult = (playerAttackValue + opponentCounterValue) * (-1)
                else
                    opponentResult = playerAttackValue * (-1)
            }
            // 방어 vs 공격 : 상대의 공격이 크면 데미지, 방어가 크면 회복
            playerType == DiceType.DEFENSE && opponentType == DiceType.ATTACK -> {
                if (opponentAttackValue > playerDefenseValue)
                    playerResult = (opponentAttackValue - playerDefenseValue) * (-1)
                else
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
                    playerResult = (opponentAttackValue + playerCounterValue) * (-1)
                else if (opponentAttackValue < playerCounterValue)
                    opponentResult = (opponentAttackValue + playerCounterValue) * (-1)
                else
                    playerResult = opponentAttackValue * (-1)
            }
            // 카운터 vs 방어 : 방어가 회복
            playerType == DiceType.COUNTER && opponentType == DiceType.DEFENSE -> {
                opponentResult = playerCounterValue
            }
            // 카운터 vs 카운터 : 더 큰 쪽이 데미지
            playerType == DiceType.COUNTER && opponentType == DiceType.COUNTER -> {
                if (playerCounterValue > opponentCounterValue)
                    opponentResult = playerCounterValue * (-1)
                else if (playerCounterValue < opponentCounterValue)
                    playerResult = opponentCounterValue * (-1)
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

        Log.d("LogTemp", curPlayerHealth.toString() + " " + curOpponentHealth.toString())

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
                    imgOpponentDiceAttack.setImageResource(imgName[attackValue - 1])
                    imgOpponentDiceDefense.setImageResource(imgName[defenseValue - 1])
                    imgOpponentDiceCounter.setImageResource(imgName[counterValue - 1])

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

                    gameplay()
                }
            }
    }

    private fun dpToPx(dp: Int, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
    }

    // 결과창 타이머 시작 함수
    private fun startResultTimer() {
        resultTimer = object : CountDownTimer(resultTimeLimit, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                db.collection("BattleRooms")
                    .document(roomName)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            document.reference.update(playerId + "Round", 0)
                            document.reference.update(playerId + "Attack", 0)
                            document.reference.update(playerId + "Defense", 0)
                            document.reference.update(playerId + "Counter", 0)
                            document.reference.update(playerId + "Choose", 0)
                        }
                    }

                var flag = false

                db.collection("BattleRooms").document(roomName)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists() && !flag) {
                            // 상대방의 round = true 인지 확인

                            val opponentRound = snapshot.getLong(opponentId + "Round") ?: 1L
                            val playerRound = snapshot.getLong(playerId + "Round") ?: 1L
                            if (opponentRound + playerRound == 0L) {
                                proceedToNextTurn()
                                flag = true
                            }
                        }
                    }
            }
        }.start()
    }

    // 라운드 타이머 시작 함수
    private fun startRoundTimer() {
        roundTimer = object : CountDownTimer(roundTimeLimit, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                txtRoundTimer.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                // Don't Selected Any Dice
                if (!isWaiting) {
                    playerRolls = rollDices()
                    var randomChoose = Random.nextInt(0, 2) // 1부터 6까지의 랜덤 숫자를 반환합니다
                    when (randomChoose) {
                        0 -> playerType = DiceType.ATTACK
                        1 -> playerType = DiceType.DEFENSE
                        2 -> playerType = DiceType.COUNTER
                    }

                    db.collection("BattleRooms")
                        .document(roomName)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                document.reference.update(playerId + "Choose", randomChoose)
                                document.reference.update(playerId + "Round", 1)
                            }
                        }

                    var opponentAttack : Int = 0
                    var opponentDefense : Int = 0
                    var opponentCounter : Int = 0
                    var opponentChoose : Int = 0

                    var flag = false

                    db.collection("BattleRooms").document(roomName)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                return@addSnapshotListener
                            }

                            if (snapshot != null && snapshot.exists() && !flag) {
                                // 상대방의 round = true 인지 확인
                                val opponentRound = snapshot.getLong(opponentId + "Round") ?: 0L
                                if (opponentRound == 1L) {

                                    flag = true

                                    opponentAttack = snapshot.getLong(opponentId + "Attack")!!.toInt()
                                    opponentDefense = snapshot.getLong(opponentId + "Defense")!!.toInt()
                                    opponentCounter = snapshot.getLong(opponentId + "Counter")!!.toInt()
                                    opponentRolls = Triple(opponentAttack, opponentDefense, opponentCounter)
                                    opponentChoose = snapshot.getLong(opponentId + "Choose")!!.toInt()
                                    when (opponentChoose) {
                                        0 -> opponentType = DiceType.ATTACK
                                        1 -> opponentType = DiceType.DEFENSE
                                        2 -> opponentType = DiceType.COUNTER
                                    }

                                    Log.d("LogTemp", opponentRolls.toString())

                                    // round timer 종료 후 게임 결과 프로세스로 넘어간다
                                    roundTimer?.cancel()
                                    showResult()
                                }
                            }
                        }
                }
                else {
                    var opponentAttack : Int = 0
                    var opponentDefense : Int = 0
                    var opponentCounter : Int = 0
                    var opponentChoose : Int = 0

                    var flag = false

                    db.collection("BattleRooms").document(roomName)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                return@addSnapshotListener
                            }

                            if (snapshot != null && snapshot.exists() && !flag) {
                                // 상대방의 round = true 인지 확인
                                val opponentRound = snapshot.getLong(opponentId + "Round") ?: 0L
                                if (opponentRound == 1L) {

                                    flag = true

                                    opponentAttack = snapshot.getLong(opponentId + "Attack")!!.toInt()
                                    opponentDefense = snapshot.getLong(opponentId + "Defense")!!.toInt()
                                    opponentCounter = snapshot.getLong(opponentId + "Counter")!!.toInt()
                                    opponentRolls = Triple(opponentAttack, opponentDefense, opponentCounter)
                                    opponentChoose = snapshot.getLong(opponentId + "Choose")!!.toInt()
                                    when (opponentChoose) {
                                        0 -> opponentType = DiceType.ATTACK
                                        1 -> opponentType = DiceType.DEFENSE
                                        2 -> opponentType = DiceType.COUNTER
                                    }

                                    // round timer 종료 후 게임 결과 프로세스로 넘어간다
                                    roundTimer?.cancel()

                                    Log.d("LogTemp", playerRolls.toString() + " " + opponentRolls.toString())
                                    showResult()
                                }
                            }
                        }
                }
            }
        }.start()
    }

    private fun exitGame() {
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
                    Log.d("LogTemp", score.toString())
                }
                else
                    Log.d("LogTemp", "점수계산안됨")
            }

        db.collection("BattleRooms").document(roomName).delete()
            .addOnSuccessListener {
                db.collection("BattleWait").document(acceptId).delete()
                    .addOnSuccessListener {
                        runOnUiThread {
                            Toast.makeText(this@InGameActivity, "Complete out of battle room", Toast.LENGTH_SHORT).show()
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
}