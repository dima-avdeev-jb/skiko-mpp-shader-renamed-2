package org.jetbrains.skiko.sample

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import org.jetbrains.skiko.*

val shader1: Shader = RuntimeEffect.makeForShader(
    """
    half4 main(float2 fragCoord) {
      //float2 scaled = fragCoord/iResolution.xy;
      return half4(1, 0.5, 0, 1);
    }
"""
).makeShader(
    uniforms = null,//Data.makeFromBytes(),
    children = null,
    localMatrix = null,
    isOpaque = false
)

class Clocks(private val layer: SkiaLayer): SkikoView {
    private val cursorManager = CursorManager()
    private val withFps = true
    private val fpsCounter = FPSCounter()
    private var xpos = 0.0
    private var ypos = 0.0
    private var xOffset = 0.0
    private var yOffset = 0.0
    private var scale = 1.0
    private var k = scale
    private val fontCollection = FontCollection()
        .setDefaultFontManager(FontMgr.default)
    private val style = ParagraphStyle()
    private var inputText = ""

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        if (withFps) fpsCounter.tick()
        canvas.translate(xOffset.toFloat(), yOffset.toFloat())
        canvas.scale(scale.toFloat(), scale.toFloat())

        val watchFill = Paint().apply {
            color
            shader = shader1
        }
        canvas.drawRect(Rect(0f, 0f, width.toFloat(), width.toFloat()), watchFill)

        val frames = ParagraphBuilder(style, fontCollection)
            .pushStyle(TextStyle().setColor(0xffFFffFFL.toInt()).setFontSize(24f))
            .addText("Peace to Ukraine!")
            .popStyle()
            .build()
        frames.layout(Float.POSITIVE_INFINITY)
        frames.paint(canvas, ((xpos - xOffset) / scale).toFloat(), ((ypos - yOffset) / scale).toFloat())

        canvas.resetMatrix()
    }

    override fun onPointerEvent(event: SkikoPointerEvent) {
        when (event.kind) {
            SkikoPointerEventKind.DOWN,
            SkikoPointerEventKind.MOVE -> {
                if (event.x > 200) {
                    cursorManager.setCursor(layer.component, PredefinedCursors.HAND)
                } else {
                    cursorManager.setCursor(layer.component, PredefinedCursors.DEFAULT)
                }
                xpos = event.x
                ypos = event.y
            }
            SkikoPointerEventKind.DRAG -> {
                xOffset += event.x - xpos
                yOffset += event.y - ypos
                xpos = event.x
                ypos = event.y
            }
            else -> {}
        }
    }

    override fun onInputEvent(event: SkikoInputEvent) {
        if (event.input != "\b") {
            inputText += event.input
        }
    }

    override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
        if (event.kind == SkikoKeyboardEventKind.DOWN) {
            when (event.key) {
                SkikoKey.KEY_NUMPAD_ADD -> scale *= 1.1
                SkikoKey.KEY_I -> {
                    if (event.modifiers == SkikoInputModifiers.SHIFT) {
                        scale *= 1.1
                    }
                }
                SkikoKey.KEY_NUMPAD_SUBTRACT -> scale *= 0.9
                SkikoKey.KEY_O -> {
                    if (event.modifiers == SkikoInputModifiers.SHIFT) {
                        scale *= 0.9
                    }
                }
                SkikoKey.KEY_NUMPAD_4,
                SkikoKey.KEY_LEFT -> xOffset -= 5.0
                SkikoKey.KEY_NUMPAD_8,
                SkikoKey.KEY_UP -> yOffset -= 5.0
                SkikoKey.KEY_NUMPAD_6,
                SkikoKey.KEY_RIGHT -> xOffset += 5.0
                SkikoKey.KEY_NUMPAD_2,
                SkikoKey.KEY_DOWN -> yOffset += 5.0
                else -> {}
            }
        }
    }

    override fun onTouchEvent(events: Array<SkikoTouchEvent>) {
        val event = events.first()
        if (event.kind == SkikoTouchEventKind.STARTED) {
            xpos = event.x
            ypos = event.y
        }
    }

    override fun onGestureEvent(event: SkikoGestureEvent) {
        when (event.kind) {
            SkikoGestureEventKind.TAP -> {
                xpos = event.x
                ypos = event.y
            }
            SkikoGestureEventKind.PINCH -> {
                if (event.state == SkikoGestureEventState.STARTED) {
                    k = scale
                }
                scale = k * event.scale
            }
            SkikoGestureEventKind.PAN -> {
                xOffset += event.x - xpos
                yOffset += event.y - ypos
                xpos = event.x
                ypos = event.y
            }
            else -> {}
        }
    }
}