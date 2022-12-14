import com.soywiz.klock.*
import com.soywiz.korau.sound.readMusic
import com.soywiz.korev.Key
import com.soywiz.korge.*
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.delay
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tween.moveTo
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.random.Random
import kotlin.reflect.KClass

suspend fun main() = Korge(Korge.Config(module = ConfigModule))

object ConfigModule : Module() {
    override val bgcolor = Colors["#2b2b2b"]
    override val size = SizeInt(1024, 768)
    override val mainScene: KClass<out Scene> = Scene1::class

    override suspend fun AsyncInjector.configure() {
        mapPrototype { Scene1() }
        mapPrototype { Scene2() }
    }
}

class Scene1() : Scene() {
    override suspend fun Container.sceneInit() {
        val bg = solidRect(1024.0, 768.0, Colors["#02020bdd"]).xy(1024.0, 768.0)

        val title = text("START GAME", alignment = TextAlignment.CENTER, textSize = 30.0).xy(IPoint.invoke(bg.width / 2, bg.height / 2 ))

        title.onClick {
            sceneContainer.changeTo<Scene2>()
        }

    }

}

class Scene2() : Scene() {
    override suspend fun Container.sceneInit() {


        // Establish background field
        val rect = solidRect(1024.0, 768.0, Colors["#02020bdd"]).xy(0.0, 0.0)

        // Some Abstract Values
        val buffer = 40
        val minDegrees = (-110).degrees
        val maxDegrees = (+90).degrees

        var enemyHits = 0
        var chipPickUps = 0
        var currentNumberValue = 0

        var chipSwitch = true
        var numberOneSwitch = true
        var enemySwitch = true
        var levelIsActive = false

        val surferBoundary = rect.height - 130

        var numberSwitch = true

        val fontOne = resourcesVfs["ClearSans-Bold.ttf"].readTtfFont()


        // SPRITES AND IMAGES

        // Target
        val neonTarget = image(resourcesVfs["neon_target_1.png"].readBitmap()) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale(.085)
            position(rect.width / 2, rect.height - 130)
        }

        // Red Triangle 1
        val redTriangleOne = resourcesVfs["red_tri_complete.xml"].readAtlas()
        val triangleOneAnimation = redTriangleOne.getSpriteAnimation("neon")

        // Red Skull 1
        val redSkullOne = resourcesVfs["red_skull.xml"].readAtlas()
        val redSkullOneAnimation = redSkullOne.getSpriteAnimation("red")

        // Chip
        val chipOneSprites = resourcesVfs["circuit_board.xml"].readAtlas()
        val chipOneAnimation = chipOneSprites.getSpriteAnimation("circuit")

        // Banner
        val rect2 = solidRect(1024.0, 65.0, Colors["#3c436df7"]).xy(0.0, 0.0)

        // HEARTS
        val heartImgOne = image(resourcesVfs["pixel_heart_one.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.03)
            position(rect.width - 160, 30.0)
            visible = true
        }

        val heartImgTwo = image(resourcesVfs["pixel_heart_one.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.03)
            position(rect.width - 120, 30.0)
        }

        val heartImgThree = image(resourcesVfs["pixel_heart_one.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.03)
            position(rect.width - 80, 30.0)
        }


        // LASER
        val laserOne = image(resourcesVfs["laser_green_one.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.07)
            position(rect.width / 2, 30.0)
            rotation(Angle.fromDegrees(90))
            visible = false
        }

        // EXPLOSION STUFF

        val spriteMap = resourcesVfs["explosion.png"].readBitmap()

        val explosionAnimation = SpriteAnimation(
            spriteMap = spriteMap,
            spriteWidth = 128, // image is 1024x1024 and it's 8x8, 1024 / 8 = 128
            spriteHeight = 128,
            marginTop = 0, // default
            marginLeft = 0, // default
            columns = 8,
            rows = 8,
            offsetBetweenColumns = 0, // default
            offsetBetweenRows = 0 // default
        )

        val explosion = sprite(explosionAnimation)
        explosion.visible = false
        explosion.scale = 1.0


        // RED TRIANGLES

        val redTriangleGroupOne = Array(1) {
            sprite(triangleOneAnimation) {
                anchor(.5, .5)
                scale(.2)
                visible = false
                this.playAnimationLooped(spriteDisplayTime = 90.milliseconds)
            }
        }

        val redTriangleGroupTwo = Array(1) {
            sprite(triangleOneAnimation) {
                anchor(.5, .5)
                scale(.2)
                visible = false
                this.playAnimationLooped(spriteDisplayTime = 90.milliseconds)
            }
        }

        val redTriangleGroupThree = Array<Sprite>(1) {
            sprite(triangleOneAnimation) {
                anchor(.5, .5)
                scale(.2)
                visible = false
                this.playAnimationLooped(spriteDisplayTime = 90.milliseconds)
            }
        }

        val redTriangleGroupFour = Array<Sprite>(1) {
            sprite(triangleOneAnimation) {
                anchor(.5, .5)
                scale(.2)
                visible = false
                this.playAnimationLooped(spriteDisplayTime = 90.milliseconds)
            }
        }

        // Red Skulls

        val redSkullGroupOne = Array<Sprite>(1) {
            sprite(redSkullOneAnimation) {
                anchor(.5, .5)
                scale(.15)
                visible = false
                this.playAnimationLooped(spriteDisplayTime = 90.milliseconds)
            }
        }

        val redSkullGroupTwo = Array<Sprite>(1) {
            sprite(redSkullOneAnimation) {
                anchor(.5, .5)
                scale(.15)
                visible = false
                this.playAnimationLooped(spriteDisplayTime = 90.milliseconds)
            }
        }
        // CAN CLUSTER

        val chipCluster = Array<Sprite>(1) {
            sprite(chipOneAnimation) {
                anchor(.5, .5)
                scale(.2)
                visible = false
                this.playAnimationLooped(spriteDisplayTime = 90.milliseconds)

            }
        }

        // NUMBERS
        var fallingValueOne = (0..29).random()
        // how do we trigger when this randomization happens (aside from level start)?  at the end of a wave? when you shoot your laser?
        // make sure it affects numberCluster as you'd want

        val numberCluster = Array<Text>(1) {

            text(fallingValueOne.toString()) {
                pos = (IPoint.invoke((rect.width / 16), -28.0))
                textSize = 38.0
                visible = false

            }
        }

        // FRAMES

        val screenFrame = image(resourcesVfs["monitor_cyberpunk_frame_1.png"].readBitmap()) {
            anchor(.5, .5)
            scale(1.85)
            position((rect.width / 2), (rect.height / 2) + 25)
        }

        val scoreFrame = image(resourcesVfs["monitor_cyberpunk_small_frame_png_v.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.79)
            position(((rect.width / 4) * 3) + 10, 60.0)
        }

        val targetFrame = image(resourcesVfs["monitor_cyberpunk_number_display.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.69)
            position((rect.width / 6), 56.0)
        }

        val currentFrame = image(resourcesVfs["monitor_cyberpunk_number_display.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.69)
            position((rect.width / 16), 56.0)
        }

        // TARGET NUMBER
        val targetNumber = text((0..99).random().toString()) {
            textSize = 46.0
            color = Colors.GREEN
            pos = (IPoint.invoke((rect.width / 6), 28.0))
            alignment = TextAlignment.CENTER
            font = fontOne
        }

        // CURRENT NUMBER
        var currentNumber = text(0.toString()) {
            textSize = 46.0
            pos = (IPoint.invoke((rect.width / 16), 28.0))
            alignment = TextAlignment.CENTER
            font = fontOne
        }

        // Establish Music

        val music = resourcesVfs["eric_track_1.wav"].readMusic()
        music.play()

        // Energy ball
        val energyBall = image(resourcesVfs["red_button_one.png"].readBitmap()) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale(.09)
            position(rect.width / 3.3, rect.height - 45)
        }


        suspend fun targetMovement(clickPoint: Point) {

            if (clickPoint.y <= surferBoundary) { clickPoint.y = surferBoundary }
            if (clickPoint.y >= surferBoundary) { clickPoint.y = surferBoundary }
            neonTarget.tweenAsync(neonTarget::x[neonTarget.x, clickPoint.x], time = 1.5.seconds, easing = Easing.EASE)
            neonTarget.tweenAsync(neonTarget::y[neonTarget.y, clickPoint.y], time = 1.5.seconds, easing = Easing.EASE)

        }


        // Level Functions

        fun levelComplete() {

            val levelComplete = text("Level Completed") {
                position(centerOnStage())
                neonTarget.removeFromParent()
                chipCluster.forEach { it.removeFromParent() }
            }
        }

        fun gameOver() {

            val gameOver = text("GAME OVER") {
                position(centerOnStage())
                neonTarget.removeFromParent()
                chipCluster.forEach { it.removeFromParent() }
            }
        }

        // track switch position for hit detection

        fun chipSwitchHit() {
            if (chipSwitch) {
                chipPickUps += 1
               // energyBall.scale += .05
            }

            // WIN Parameters
            if (chipPickUps >= 3) {
                levelComplete()
            }
        }

        // Numbers update method

        fun currentNumberUpdate() {
            if (numberOneSwitch) {
                currentNumberValue += fallingValueOne
            }
            currentNumber.text = currentNumberValue.toString()
        }

        fun enemyHit() {
            if (enemySwitch) {
                enemyHits += 1
            }
            if (enemyHits == 1) {
                heartImgThree.visible = false
            }

            if (enemyHits == 2) {
                heartImgTwo.visible = false
            }

            if (enemyHits >= 3) {
                heartImgOne.visible = false
                gameOver()
            }
        }

        suspend fun laserBoi() {
            laserOne.position(neonTarget.x, neonTarget.y)
            laserOne.visible = true
            laserOne.moveTo(laserOne.x, -25.0, 0.5.seconds, Easing.EASE)
        }

        fun switchOperator() {
            numberSwitch = !numberSwitch
            println("number switch is set to $numberSwitch")
        }

        suspend fun enemyWaveOne() {

            println("Enemy Wave 1")
            awaitAll(async {
                // Red Triangle Group 1
                redTriangleGroupOne.forEach {
                    
                    delay((Random.nextInt(1, 2)).seconds)
                    val triangleX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(triangleX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(triangleX + 75, 700.0, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(triangleX + 3, height - 25, 3.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(triangleX + 30, height + buffer, 1.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Triangle Group 2
                redTriangleGroupTwo.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 3.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 3, height - 73, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 30, height + buffer, 1.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Triangle Group 3
                redTriangleGroupThree.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 300.0, 3.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 3, height - buffer, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 30, height + buffer, 1.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Triangle Group 4
                redTriangleGroupFour.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 3, height - buffer, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 30, height + buffer, 2.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Skull Group 1
                redSkullGroupOne.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 3.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 3, height - 73, 2.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 30, height + buffer, 2.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Skull Group 2
                redSkullGroupTwo.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 3.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 3, height - 73, 2.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 30, height + buffer, 2.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Chip Cluster
                chipCluster.forEach {
                    //  if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    chipSwitch = true
                    val canX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    it.visible = true
                    it.position(canX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this)) {
                            this.visible = false
                            chipSwitchHit()
                            chipSwitch = false

                            // colorDefault = AnsiEscape.Color.RED
                            println("chip pick-ups: $chipPickUps")
                        }
                    }

                    awaitAll(async {it.tween(it::rotation[270.degrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)},
                           async{it.moveTo(canX, height + buffer, 6.seconds, Easing.EASE_IN)})


                    // 7 Seconds

                }
            }, async {
                // Number
                numberCluster.forEach {
                    //  if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    fallingValueOne = (0..29).random()
                    it.text = fallingValueOne.toString()
                    numberOneSwitch = true
                    val canX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    it.visible = true
                    it.position(canX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this)) {
                            this.visible = false
                            currentNumberUpdate()
                            numberOneSwitch = false

                        }
                    }

                    awaitAll(async{it.moveTo(canX, height + buffer, 7.seconds, Easing.EASE_IN)})


                    // 7 Seconds

                }

            })
        }
        suspend fun runDataTwo() {

            println("DATA RUNNING")
            awaitAll(async {
                // Red Triangle Group 1
                redTriangleGroupOne.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 700.0, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 3, height - 25, 3.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 30, height + buffer, 1.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Triangle Group 2
                redTriangleGroupTwo.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 3.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 3, height - 73, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 30, height + buffer, 1.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Triangle Group 3
                redTriangleGroupThree.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 300.0, 3.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 3, height - buffer, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 30, height + buffer, 1.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Triangle Group 4
                redTriangleGroupFour.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[maxDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 3, height - buffer, 2.seconds, Easing.EASE_IN)
                    it.tween(it::rotation[minDegrees], time = 500.milliseconds, easing = Easing.EASE_IN_OUT)
                    it.moveTo(jellyX + 30, height + buffer, 2.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Skull Group 1
                redSkullGroupOne.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 3.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 3, height - 73, 2.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 30, height + buffer, 2.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Red Skull Group 2
                redSkullGroupTwo.forEach {
                    // if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    val jellyX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    enemySwitch = true
                    it.visible = true
                    it.position(jellyX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this) && enemySwitch) {

                            var collisionPosX = neonTarget.x - 60
                            var collisionPosY = neonTarget.y - 70
                            explosion.xy(collisionPosX, collisionPosY)
                            println(collisionPosY)
                            enemyHit()
                            enemySwitch = false

                            explosion.visible = true
                            this.visible = false

                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                            println("enemy hits $enemyHits")
                        }

                        else if (laserOne.collidesWith(this)) {
                            this.visible = false
                            enemySwitch = false
                            explosion.xy(this.x - 50, this.y - 50)
                            explosion.visible = true
                            explosion.playAnimationForDuration(2.seconds)
                            explosion.onAnimationCompleted { explosion.visible = false}

                        }

                    }

                    it.moveTo(jellyX + 75, 400.0, 3.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 3, height - 73, 2.seconds, Easing.EASE_IN)
                    it.moveTo(jellyX + 30, height + buffer, 2.seconds, Easing.EASE_IN)

                    // 7 Seconds

                }
            }, async {
                // Chip Cluster
                chipCluster.forEach {
                    //  if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    chipSwitch = true
                    val canX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    it.visible = true
                    it.position(canX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this)) {
                            this.visible = false
                            chipSwitchHit()
                            chipSwitch = false

                            // colorDefault = AnsiEscape.Color.RED
                            println("chip pick-ups: $chipPickUps")
                        }
                    }

                    awaitAll(async {it.tween(it::rotation[270.degrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)},
                        async{it.moveTo(canX, height + buffer, 6.seconds, Easing.EASE_IN)})


                    // 7 Seconds

                }
            }, async {
                // Number
                numberCluster.forEach {
                    //  if (!it.visible || it.pos.y > height) {
                    delay((Random.nextInt(1, 2)).seconds)
                    fallingValueOne = (0..29).random()
                    it.text = fallingValueOne.toString()
                    numberOneSwitch = true
                    val canX = Random.nextInt(buffer, (width.toInt() - buffer)).toDouble()
                    it.visible = true
                    it.position(canX, -5.0)

                    it.addUpdater {
                        if (neonTarget.collidesWith(this)) {
                            this.visible = false
                            currentNumberUpdate()
                            numberOneSwitch = false

                        }
                    }

                    awaitAll(async{it.moveTo(canX, height + buffer, 7.seconds, Easing.EASE_IN)})


                    // 7 Seconds

                }

            })
        } // adjust this method to use new groups (triangles, skulls) and switch number color to red and see if it adds to enemy flow

        // create a sprite map with various levels of fuzz opacity on main logo for an opening animation

        suspend fun jellyTimer() {
            while (levelIsActive) {
                awaitAll(

                    async { enemyWaveOne() },
                    async {
                       energyBall.tween(energyBall::rotation[minDegrees], time = 5.seconds, easing = Easing.EASE_IN_OUT)
                        energyBall.tween(energyBall::rotation[maxDegrees], time = 3.seconds, easing = Easing.EASE_IN_OUT) },
                    async { runDataTwo() },
                    async {
                        neonTarget.tween(neonTarget::rotation[minDegrees], time = 4.seconds, easing = Easing.EASE_IN_OUT)
                       neonTarget.tween(neonTarget::rotation[maxDegrees], time = 4.seconds, easing = Easing.EASE_IN_OUT) }
                )
            }
        }



        // INPUTS

        rect.onClick {

            println("clicked!")

            val target = it.currentPosLocal

            // MOVE TARGET
            neonTarget.position(neonTarget.x, neonTarget.y)
            targetMovement(target)

        }

        addUpdater {

            if (views.input.keys[Key.SPACE]) {
                async { laserBoi() }
            }

        }

        this.keys {
            down(Key.O) { switchOperator() }
            // up(Key.LEFT) { e -> /*...*/ }
        }



        energyBall.onClick {
            levelIsActive = true
            println(levelIsActive.equals(true))
            jellyTimer()
        }
    }
}