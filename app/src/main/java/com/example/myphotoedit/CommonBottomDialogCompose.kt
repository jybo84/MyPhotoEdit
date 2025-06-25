package com.example.myphotoedit

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun TestShowDialog() {
    DialogBottom(
        onNegativeButtonClick = {},
        onPositiveButtonClick = { },
        textNegativeButton = "верх ",
        textPositiveButton = " низ"
    )
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun DialogBottom(
    onNegativeButtonClick: () -> Unit,
    onPositiveButtonClick: () -> Unit,
    textNegativeButton: String,
    textPositiveButton: String
) {

    val dialogBackgroundColor = Color(0xFFF5F5F5).copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dialogBackgroundColor)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(dialogBackgroundColor)
                .padding(horizontal = 8.dp, vertical = 16.dp),

            verticalArrangement = Arrangement.spacedBy(4.dp)
        )
        {
            ButtonOnDialog(
                onButtonDialogClick = onNegativeButtonClick,
                textOnButton = textNegativeButton.uppercase()
            )

            ButtonOnDialog(
                onButtonDialogClick = onPositiveButtonClick,
                textOnButton = textPositiveButton.uppercase()
            )
        }
    }
}

@Composable
fun ButtonOnDialog(
    onButtonDialogClick: () -> Unit,
    textOnButton: String
) {

    val robotoRegular = FontFamily(Font(R.font.roboto_regular))

    Button(
        onClick = onButtonDialogClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {

        Text(
            textOnButton.uppercase(),
            fontSize = 15.sp,
            fontFamily = robotoRegular,
        )
    }
}
