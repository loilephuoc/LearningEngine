package vn.loi.learning.desktop.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.navigation.NavigationDestination

@Composable
fun Sidebar(
    currentDestination: NavigationDestination,
    onDestinationSelected: (NavigationDestination) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavigationDestination.entries.forEach { destination ->
                val accessibility =
                    resolveSidebarDestinationAccessibility(
                        destination = destination,
                        currentDestination = currentDestination
                    )

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .semantics(
                                mergeDescendants = true
                            ) {
                                role = Role.Tab
                                selected = accessibility.selected
                                contentDescription =
                                    accessibility.contentDescription
                            }
                            .clickable {
                                onDestinationSelected(destination)
                            },
                    shape = RoundedCornerShape(12.dp),
                    color =
                        if (accessibility.selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                ) {
                    Text(
                        text = accessibility.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (accessibility.selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                    )
                }
            }
        }
    }
}