package vn.loi.learning.desktop.ui.reviewhistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.search.SearchField
import vn.loi.learning.desktop.ui.state.*

@Composable fun ReviewHistoryScreen(uiState: ReviewHistoryUiState,onRetry:()->Unit,onQueryChanged:(String)->Unit,onClearQuery:()->Unit,onFilterChanged:(ReviewHistoryFilter)->Unit,onSortChanged:(ReviewHistorySort)->Unit,modifier:Modifier=Modifier){
 Column(modifier,verticalArrangement=Arrangement.spacedBy(16.dp)){
  DesktopLoadStateCard(uiState.loadState,"Review History",onRetry)
  if(uiState.loadState!=DesktopLoadState.Loading){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Review History",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(reviewHistorySearchSummary(uiState).label)}
   SearchField(uiState.query,"Search review history",reviewHistorySearchSummary(uiState),onQueryChanged,onClearQuery)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ReviewHistoryFilter.entries.forEach{f->FilterChip(selected=uiState.filter==f,onClick={onFilterChanged(f)},label={Text(f.label)})}}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ReviewHistorySort.entries.forEach{s->FilterChip(selected=uiState.sort==s,onClick={onSortChanged(s)},label={Text(s.label)})}}
   HorizontalDivider()
   if(uiState.visibleItems.isEmpty()) Box(Modifier.fillMaxSize().semantics{contentDescription=reviewHistoryEmptySearchMessage(uiState)},contentAlignment=Alignment.Center){Text(reviewHistoryEmptySearchMessage(uiState))}
   else LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)){items(uiState.visibleItems){ReviewHistoryCard(it)}}
  }
 }
}
@Composable private fun ReviewHistoryCard(item:ReviewHistoryItemUi){Card(Modifier.fillMaxWidth().semantics(mergeDescendants=true){contentDescription=resolveReviewHistoryItemContentDescription(item)}){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(item.rating,fontWeight=FontWeight.SemiBold);Text(item.reviewedAt)};HorizontalDivider();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)){Metric("Response time",item.responseTime,Modifier.weight(1f));Metric("Stability",item.stability,Modifier.weight(1f));Metric("Difficulty",item.difficulty,Modifier.weight(1f))}}}}
@Composable private fun Metric(label:String,value:String,modifier:Modifier=Modifier){Column(modifier){Text(label,style=MaterialTheme.typography.labelMedium);Text(value,fontWeight=FontWeight.Medium)}}
