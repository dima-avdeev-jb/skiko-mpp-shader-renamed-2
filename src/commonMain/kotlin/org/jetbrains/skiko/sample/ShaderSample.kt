package org.jetbrains.skiko.sample

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import org.jetbrains.skiko.*


//Thanks to https://www.shadertoy.com/view/7sBBRV
val sksl = """
uniform float2 iResolution;
uniform float iTime;
uniform shader content;
float hash13(vec3 p3) {
    p3  = fract(p3 * .1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}
half4 main(float2 fragCoord) {
vec2 uv = fragCoord/iResolution.xy;
uv = (uv-0.5) *(1.2 - uv.x*0.05) + vec2(0.5, 0.5);
vec3 windSeed =  vec3(uv.x * 5.,uv.y * 7., iTime);
float BORDER = 0.1;
float pole = smoothstep(BORDER, 1., uv.x + 0.2);
pole = 1. - pow(1.-pole,6.);
float windForce = 0.2 + abs(cos(iTime * 0.2 + pole)  * cos(iTime * 0.345));
float flagUp = 1.-windForce;
uv.y += pole * (flagUp * 0.1 - 0.06);
uv.x -= pole * flagUp * 0.025; // fabric stretching -> flag length
vec3 gyrPos = vec3(uv.x * (1. + pole * pole *2.), uv.y, iTime * 0.4) * 3.;
float gyr = smoothstep(0.,1., abs(dot(sin(gyrPos), cos(gyrPos.zxy))));
float wind = 
    sin(iTime * (10.45) + gyr - uv.x*(20.))*windForce* 0.5
    // Wind phases 
   +cos(iTime * 1.23 - uv.y * 3. + uv.x * 3.) * 0.5; // perspective waving
uv += wind*0.03 * pole;
float w = 1.0; //length(vec2(dFdx(uv.x), dFdy(uv.y)));
float isFlag = smoothstep(BORDER,BORDER + .01, 0.7 - abs(uv.x-0.25)) * smoothstep(BORDER,BORDER + .01, 0.5- abs(uv.y-0.5));
float shadowThickness = 0.3 * (1. + wind*0.15);
vec2 shadowUv = (uv-0.5)*1.3 + vec2(-0.15, 0.15);

float shadow = smoothstep(BORDER - shadowThickness, BORDER, 0.7 - abs(shadowUv.x + 0.27)) *
  smoothstep(BORDER - shadowThickness, BORDER, 0.5 - abs(shadowUv.y));
float isBlue = smoothstep(-1.0, 1.0, (0.5 - uv.y)*200);
vec3 col = mix(vec3(1., 0.84, 0.) , vec3(0.,0.2,0.8), isBlue); // Yelow Blue
float upaEdge = cos(iTime);// 0.2;
col = sqrt(col);
col.rgb *= (0.8 + wind * 0.3 * pole); // shadow
col.rgb += 0.1 * pow(max(0., wind),7. + 10.) * vec3(0.3, 0.3, 0.6);  // blick
float bg = 0.3;
col.rgb = mix(vec3(bg) * (2. - shadow), col.rgb, isFlag);
float noise =  hash13(vec3(fragCoord * 15.0, iTime));
col.rgb += vec3(noise) * 0.03; // hide color range limit on shadows
col.rgb += col.gbr*col.brg * 0.3; // Bloom
return half4(col,1.0);
}
"""

val runtimeEffect = RuntimeEffect.makeForShader(sksl)
val shaderBuilder = RuntimeShaderBuilder(runtimeEffect)

class ShaderSample(private val layer: SkiaLayer): SkikoView {
    private val cursorManager = CursorManager()
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
        fpsCounter.tick()
        println("fps: ${fpsCounter.average}")
        val w = minOf(width, height).toFloat()
        val h = minOf(width, height).toFloat()
        shaderBuilder.uniform("iTime", (nanoTime % 1_000_000_000_000) / 1e9f)
        shaderBuilder.uniform("iResolution", w, h)
        val imageFilter2 = ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = shaderBuilder,
            shaderName = "content",
            input = null
        )

        val watchFill = Paint().apply {
            color
            shader
            imageFilter = imageFilter2
        }
        canvas.drawRect(Rect(0f, 0f, w, h), watchFill)

        val frames = ParagraphBuilder(style, fontCollection)
            .pushStyle(TextStyle().setColor(0xff99ff99L.toInt()).setFontSize(24f))
            .addText("Peace to Ukraine!")
            .popStyle()
            .build()
        frames.layout(Float.POSITIVE_INFINITY)
        frames.paint(canvas, 0f, 0f)

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