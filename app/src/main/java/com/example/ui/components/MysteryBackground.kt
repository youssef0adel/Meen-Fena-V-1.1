package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DeepCrimson

@Composable
fun MysteryBackground(
    modifier: Modifier = Modifier,
    drawBloodDrips: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF48100A), // Center crimson glow
                        Color(0xFF140302), // Dark ambient mid
                        Color(0xFF090000)  // Pitch black corners
                    ),
                    center = Offset.Unspecified,
                    radius = 1800f
                )
            )
            .drawBehind {
                if (drawBloodDrips) {
                    val path = Path()
                    val crimsonInk = Color(0xFF6E120A)
                    val darkBlood = Color(0xFF3B0703)
                    
                    // Procedural dripping blood border at the top
                    path.moveTo(0f, 0f)
                    
                    // Left to right drops
                    var currentX = 0f
                    val dripWidth = size.width / 12f
                    path.lineTo(0f, 60f)
                    
                    for (i in 0..12) {
                        val nextX = currentX + dripWidth
                        val midX = currentX + (dripWidth / 2)
                        val dropHeight = if (i % 3 == 0) 140f else if (i % 2 == 0) 100f else 70f
                        
                        // Bezier curve to simulate physical drips
                        path.cubicTo(
                            midX - (dripWidth/4), dropHeight + 30f,
                            midX + (dripWidth/4), dropHeight + 30f,
                            nextX, 60f
                        )
                        currentX = nextX
                    }
                    path.lineTo(size.width, 0f)
                    path.close()
                    
                    // Draw outer deep shadow blood
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(darkBlood, crimsonInk),
                            startY = 0f,
                            endY = 160f
                        )
                    )
                }
            }
    ) {
        content()
    }
}
