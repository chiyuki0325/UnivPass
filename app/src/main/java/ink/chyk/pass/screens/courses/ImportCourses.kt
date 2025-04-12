package ink.chyk.pass.screens.courses

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ink.chyk.pass.R
import ink.chyk.pass.activities.ImportCoursesActivity


@Composable
fun ImportCoursesSplash() {
  val ctx = LocalContext.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            painter = painterResource(R.drawable.ic_fluent_arrow_download_24_regular),
            contentDescription = "Import Courses",
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            ctx.getString(R.string.import_courses),
            style = MaterialTheme.typography.headlineMedium
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(ctx.getString(R.string.import_courses_splash_1))
        Text(ctx.getString(R.string.import_courses_splash_2))
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          ImportCoursesButton()
        }
      }
    }
  }
}

@Composable
fun ImportCoursesButton(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  Button(
    modifier = modifier,
    onClick = {
      val intent = Intent(context, ImportCoursesActivity::class.java)
      context.startActivity(intent)
    }) { Text(context.getString(R.string.import_courses)) }
}

