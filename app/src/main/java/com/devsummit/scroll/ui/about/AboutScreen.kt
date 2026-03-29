package com.devsummit.scroll.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devsummit.scroll.R
import com.devsummit.scroll.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.headlineLarge,
            color = OffWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
        )

        // Logo + App info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Teal400.copy(alpha = 0.2f), DeepTeal)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Unscroll Logo",
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unscroll",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )

                Text(
                    text = "v1.1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A digital wellness app that helps you reclaim your time by creating mindful friction before doom-scrolling takes over.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hackathon Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                AboutInfoRow(
                    icon = Icons.Outlined.EmojiEvents,
                    title = "Hackathon",
                    subtitle = "Jagannath University DevSummit Hackathon"
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = ElevatedSlate, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                AboutInfoRow(
                    icon = Icons.Outlined.Groups,
                    title = "Team",
                    subtitle = "StrangeX"
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = ElevatedSlate, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                AboutInfoRow(
                    icon = Icons.Outlined.School,
                    title = "University",
                    subtitle = "Jagannath University"
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = ElevatedSlate, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                AboutInfoRow(
                    icon = Icons.Outlined.Code,
                    title = "Built With",
                    subtitle = "Kotlin • Jetpack Compose • Material 3"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepTeal.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Our Mission",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Teal400
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We believe that technology should empower, not enslave. Unscroll puts you back in control by creating a moment of pause — a reality check — before mindless scrolling steals your most precious resource: time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OffWhite.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Made with ❤ by Team StrangeX",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleGray
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Teal400.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Teal400,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OffWhite
            )
        }
    }
}
