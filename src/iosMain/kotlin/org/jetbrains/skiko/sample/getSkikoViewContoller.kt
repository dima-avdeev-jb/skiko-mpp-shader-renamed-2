package org.jetbrains.skiko.sample

import org.jetbrains.skiko.*
import platform.UIKit.*

fun makeApp(skiaLayer: SkiaLayer) = ShaderSample(skiaLayer)

fun getSkikoViewContoller(): UIViewController = SkikoViewController(
    SkikoUIView(
        SkiaLayer().apply {
            gesturesToListen = SkikoGestureEventKind.values()
            skikoView = GenericSkikoView(this, makeApp(this))
        }
    )
)
