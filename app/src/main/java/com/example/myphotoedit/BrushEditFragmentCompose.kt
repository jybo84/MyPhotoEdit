package com.example.myphotoedit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import coil.compose.AsyncImage
import java.io.IOException
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb

@Preview
@Composable
fun BrushTest() {
    BrushEditScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BrushEditScreen() {

    var dialogStateSave by remember { mutableStateOf<Boolean>(false) }
    var dialogStateBack by remember { mutableStateOf<Boolean>(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ToolbarCompose(
                        "какой-то текст",
                        buttonBackClick = { dialogStateBack = true },
                        buttonSaveClick = { dialogStateSave = true }
                    )
                }
            )
        }
    ) {
        CanvasScreen(
            dialogStateSave = dialogStateSave,
            dialogStateBack = dialogStateBack,
            onCloseDialog = {
                dialogStateBack = false
                dialogStateSave = false
            },
            onCancelChange = { dialogStateBack = false }
        )
    }
}

@SuppressLint("Recycle")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    dialogStateSave: Boolean,
    dialogStateBack: Boolean,
    onCloseDialog: () -> Unit,
    onCancelChange: () -> Unit

) {
    val context = LocalContext.current
    var imageLoad by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    var brushLine by rememberSaveable { mutableStateOf<List<Line>>(listOf()) }

    val view = LocalView.current

    fun saveImageToGallery() {
        if (imageLoad == null) return
        val bitmap = createBitmap(imageLoad!!.width, imageLoad!!.height)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(imageLoad!!, 0f, 0f, null)

        val scaleX = imageLoad!!.width.toFloat() / view.width.toFloat()
        val scaleY = imageLoad!!.height.toFloat() / view.height.toFloat()

        brushLine.forEach { line ->
            val path = android.graphics.Path()
            line.listPoints.forEachIndexed { index, point ->
                val scaledPoint = Offset(point.x * scaleX, point.y * scaleY)
                if (index == 0 && line.isFirstPoint) {
                    path.moveTo(scaledPoint.x, scaledPoint.y)
                } else {
                    path.lineTo(scaledPoint.x, scaledPoint.y)
                }
            }

            val paint = Paint().apply {

                color = line.color.toArgb()
                strokeWidth = line.width * scaleX
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }

            canvas.drawPath(path, paint)
        }

        saveBitmapToGallery(context, bitmap)
        onCloseDialog()
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    imageLoad = BitmapFactory.decodeStream(stream)
                }
            }
        }
    )

    LaunchedEffect(true) {
        imageLauncher.launch("image/*")
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        var brushWightState by rememberSaveable { mutableStateOf(10f) }
        var drawingMode by rememberSaveable { mutableStateOf<DrawingMode>(DrawingMode.Brush) }
        var hue by remember { mutableStateOf(0f) }

        Column(modifier = Modifier.fillMaxSize())
        {
            Box(Modifier.weight(1f)) {
                AsyncImage(
                    model = imageLoad,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(drawingMode, brushWightState) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (drawingMode == DrawingMode.Eraser) {
                                        brushLine = eraseLinesAtPoint(brushLine, offset, brushWightState)
                                    } else {
                                        brushLine = brushLine + Line(
                                            listPoints = listOf(offset),
                                            isFirstPoint = true,
                                            color = Color.hsl(hue, 1f, 0.5f),
                                            width = brushWightState
                                        )
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (drawingMode == DrawingMode.Eraser) {
                                        // Обрабатываем все точки жеста для ластика
                                        val allPoints = change.historical.map { it.position } + change.position
                                        var currentLines = brushLine
                                        allPoints.forEach { point ->
                                            currentLines = eraseLinesAtPoint(currentLines, point, brushWightState)
                                        }
                                        brushLine = currentLines
                                    } else {
                                        // Обычный режим рисования
                                        val lastLine = brushLine.lastOrNull()
                                        if (lastLine != null) {
                                            val newPoints = lastLine.listPoints +
                                                    change.historical.map { it.position } +
                                                    change.position
                                            brushLine = brushLine.dropLast(1) +
                                                    lastLine.copy(listPoints = newPoints)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    // Отрисовка линий
                    brushLine.forEach { line ->
                        val path = androidx.compose.ui.graphics.Path().apply {
                            line.listPoints.forEachIndexed { index, point ->
                                if (index == 0 && line.isFirstPoint) {
                                    moveTo(point.x, point.y)
                                } else {
                                    lineTo(point.x, point.y)
                                }
                            }
                        }
                        drawPath(
                            path = path,
                            color = line.color,
                            style = Stroke(line.width)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.Bottom
            ) {

                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .weight(2f)
                        .padding(bottom = 10.dp)
                        .fillMaxHeight()
                ) {

                    Slider(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth(),
                        value = brushWightState,
                        valueRange = 2f..50f,
                        onValueChange = { newWigth ->
                            brushWightState = newWigth
                        },
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.DarkGray,
                            inactiveTickColor = Color.LightGray,
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(70.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = List(36) { i ->
                                        Color.hsl(i * 10f, 1f, 0.5f)
                                    }
                                )
                            )
                    ) {
                        Slider(
                            value = hue,
                            onValueChange = { hue = it },
                            valueRange = 0f..360f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = remember { MutableInteractionSource() },
                                    colors = SliderDefaults.colors(thumbColor = Color.White),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .shadow(4.dp, CircleShape, clip = true)
                                        .border(2.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .height(IntrinsicSize.Min)
                        .background(Color.Cyan)
                ) {

                    ButtonMode(
                        buttonClick = { drawingMode = DrawingMode.Brush },
                        colorButton = ButtonDefaults.buttonColors(
                            contentColor = if (drawingMode == DrawingMode.Brush) Color.Black else Color.LightGray,
                            disabledContentColor = if (drawingMode == DrawingMode.Brush) Color.Green else Color.White,
                            containerColor = Color.White
                        ),
                        imageButton = painterResource(R.drawable.ic_brush)
                    )

                    ButtonMode(
                        buttonClick = { drawingMode = DrawingMode.Eraser },
                        colorButton = ButtonDefaults.buttonColors(
                            contentColor = if (drawingMode == DrawingMode.Eraser) Color.Black else Color.LightGray,
                            disabledContentColor = if (drawingMode == DrawingMode.Eraser) Color.Green else Color.White,
                            containerColor = Color.White
                        ),
                        imageButton = painterResource(R.drawable.ic_eraser)
                    )
                }
            }
        }

        if (dialogStateSave) DialogBottom(
            onNegativeButtonClick = onCloseDialog,
            onPositiveButtonClick = { saveImageToGallery() },
            textNegativeButton = "ПРОДОЛЖИТЬ РЕДАКТИРОВАНИЕ",
            textPositiveButton = "ПРИМЕНИТЬ ИЗМЕНЕНИЯ"
        ) else if (dialogStateBack) DialogBottom(
            onNegativeButtonClick = {
                brushLine = emptyList()
                onCancelChange()
            },
            onPositiveButtonClick = onCloseDialog,
            textNegativeButton = "ОТМЕНИТЬ ИЗМЕНЕНИЯ",
            textPositiveButton = "ПРОДОЛЖИТЬ РЕДАКТИРОВАНИЕ"
        )
    }
}

@Composable
fun ButtonMode(
    buttonClick: () -> Unit,
    colorButton: ButtonColors,
    imageButton: Painter
) {
    Button(
        modifier = Modifier
            .fillMaxHeight()
            .width(72.dp),
        onClick = buttonClick,
        colors = colorButton,
        shape = RectangleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            painter = imageButton,
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )
    }
}

data class Line(
    val listPoints: List<Offset>,
    val color: Color,
    val width: Float,
    val isFirstPoint: Boolean,
)

enum class DrawingMode {
    Brush, Eraser
}

@Composable
fun ToolbarCompose(
    toolbarText: String,
    buttonBackClick: () -> Unit,
    buttonSaveClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        ButtonOnToolBar(
            buttonClick = buttonBackClick,
            iconPainter = painterResource(R.drawable.ic_arrow_back)
        )

        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.Center

        ) {
            Text(
                modifier = Modifier,
                text = toolbarText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        ButtonOnToolBar(
            buttonClick = buttonSaveClick,
            iconPainter = painterResource(R.drawable.ic_accept)
        )
    }
}

@Composable
fun ButtonOnToolBar(
    buttonClick: () -> Unit,
    iconPainter: Painter
) {
    Button(
        modifier = Modifier,
        onClick = buttonClick,
        shape = RectangleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.Black,
            containerColor = Color.White
        )
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = "ok",
            modifier = Modifier.size(35.dp)
        )
    }
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: return

    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)) {
                throw IOException("Не удалось сохранить изображение")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
        }

        Toast.makeText(context, "Изображение сохранено", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        context.contentResolver.delete(uri, null, null)
        Toast.makeText(context, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun eraseLinesAtPoint(lines: List<Line>, point: Offset, eraserWidth: Float): List<Line> {
    val eraserRadius = eraserWidth / 2
    val eraserRect = Rect(
        left = point.x - eraserRadius,
        top = point.y - eraserRadius,
        right = point.x + eraserRadius,
        bottom = point.y + eraserRadius
    )

    return lines.flatMap { line ->
        // Разбиваем линию на сегменты, которые не пересекаются с ластиком
        val segments = mutableListOf<MutableList<Offset>>()
        var currentSegment = mutableListOf<Offset>()

        line.listPoints.forEachIndexed { index, linePoint ->
            if (!eraserRect.contains(linePoint)) {
                currentSegment.add(linePoint)
            } else {
                if (currentSegment.isNotEmpty()) {
                    segments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
            }

            // Добавляем последний сегмент
            if (index == line.listPoints.lastIndex && currentSegment.isNotEmpty()) {
                segments.add(currentSegment)
            }
        }

        // Создаем новые линии из оставшихся сегментов
        segments.map { segment ->
            if (segment.size >= 2) {
                line.copy(
                    listPoints = segment,
                    isFirstPoint = segment.first() == line.listPoints.first()
                )
            } else {
                null
            }
        }.filterNotNull()
    }
}