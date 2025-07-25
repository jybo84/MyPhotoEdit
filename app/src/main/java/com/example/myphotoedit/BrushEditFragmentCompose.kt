package com.example.myphotoedit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
        var brushLine by rememberSaveable { mutableStateOf<List<Line>>(listOf()) }
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
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { it: Offset ->
                                    brushLine = brushLine + Line(
                                        listPoints = listOf(it),
                                        isFirstPoint = true,

                                        color = if (drawingMode == DrawingMode.Brush)
                                            Color.hsl(hue, 1f, 0.5f)
                                        else Color.Transparent,
                                        width = brushWightState
                                    )
                                },

                                onDrag = { change, dragAmount ->
                                    val firstPoint = brushLine.last()
                                    val bodyLine = change.historical.map { it.position }
                                    val lastPoint = change.position

                                    val newLine = firstPoint.listPoints + bodyLine + lastPoint

                                    brushLine = brushLine.dropLast(1) + firstPoint.copy(listPoints = newLine)

                                }
                            )
                        }
                ) {
                    brushLine.forEach { line: Line ->

                        val path = Path()
                        line.listPoints.forEachIndexed { index, point ->
                            if (index == 0 && line.isFirstPoint) {
                                path.moveTo(point.x, point.y)
                            } else
                                path.lineTo(point.x, point.y)
                        }
                        drawPath(
                            color = line.color,
                            style = Stroke(line.width),
                            path = path
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
            onPositiveButtonClick = { myToast(context) },
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

fun myToast(context: Context){
    Toast.makeText(context, "Логика не реализована", Toast.LENGTH_LONG).show()
}
