package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.random.Random

// Creates a realistic torn papyrus border shape procedurally
fun createTornPaperShape(seed: Long = 42L): GenericShape {
    return GenericShape { size, _ ->
        val rand = Random(seed)
        val numPoints = 60
        
        // Start top-left
        moveTo(0f, 0f)
        
        // Top edge: wiggle
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f)
            lineTo(x, y)
        }
        
        // Right edge: wiggle
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = size.width + (if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f))
            lineTo(x, y)
        }
        
        // Bottom edge: wiggle
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = size.height + (if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f))
            lineTo(x, y)
        }
        
        // Left edge: wiggle
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f)
            lineTo(x, y)
        }
        
        close()
    }
}

@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    seed: Long = 100L,
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val tornShape = remember(seed) { createTornPaperShape(seed) }
    
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = tornShape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color(0xFF1E140B)
            )
            .clip(tornShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(PapyrusBgLight, PapyrusBg),
                    center = Offset.Unspecified
                )
            )
            .drawBehind {
                // Procedural vintage papyrus frame ink lines
                val path = android.graphics.Path()
                val rand = Random(seed + 1)
                
                // Draw a rustic inner frame line with fine hand-drawn jitters
                val inset = 12f
                path.moveTo(inset, inset)
                
                // Top inner side
                path.lineTo(size.width - inset, inset + (rand.nextFloat() * 4f - 2f))
                // Right inner side
                path.lineTo(size.width - inset + (rand.nextFloat() * 4f - 2f), size.height - inset)
                // Bottom inner side
                path.lineTo(inset, size.height - inset + (rand.nextFloat() * 4f - 2f))
                // Left inner side
                path.lineTo(inset + (rand.nextFloat() * 4f - 2f), inset)
                
                drawPath(
                    path = path.asComposePath(),
                    color = Color(0x3B2C1E14),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
                    )
                )
            }
            .padding(contentPadding)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun ParchmentHeaderBanner(
    text: String,
    modifier: Modifier = Modifier,
    seed: Long = 777L
) {
    val tornShape = remember(seed) { createTornPaperShape(seed) }
    Box(
        modifier = modifier
            .wrapContentSize()
            .shadow(3.dp, tornShape)
            .background(PapyrusBgLight)
            .border(1.dp, Color(0xFF422112), tornShape)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF4E160E),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = HandjetFontFamily,
            textAlign = TextAlign.Center
        )
    }
}
