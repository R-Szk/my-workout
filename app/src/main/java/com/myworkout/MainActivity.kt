package com.myworkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myworkout.feature.catalog.*
import com.myworkout.feature.home.HomeScreen
import com.myworkout.feature.summary.*
import com.myworkout.feature.workout.*
import com.myworkout.ui.theme.WorkoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            WorkoutTheme {
                val catalog:CatalogViewModel=viewModel()
                val catalogState by catalog.state.collectAsStateWithLifecycle()
                val workout:WorkoutViewModel=viewModel()
                val state by workout.state.collectAsStateWithLifecycle()
                val summary:WorkoutSummaryViewModel=viewModel()
                val summaryState by summary.state.collectAsStateWithLifecycle()
                LaunchedEffect(state.summaryId) { summary.select(state.summaryId) }
                Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                    Box(Modifier.safeDrawingPadding()) {
                        when {
                            state.showWorkout && state.active!=null -> WorkoutScreen(state,workout)
                            state.summaryId!=null -> {
                                val report=summaryState.summary
                                if(report!=null && report.history.session.id==state.summaryId) WorkoutSummaryScreen(report,workout::closeSummary)
                                else Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center) {
                                    if(summaryState.error) Text("記録を読み込めませんでした。") else CircularProgressIndicator()
                                    TextButton(onClick=summary::retry){Text("再読み込み")}
                                    TextButton(onClick=workout::closeSummary){Text("ホームへ")}
                                }
                            }
                            else -> HomeScreen(catalogState,state,workout,catalog::load,summaryState.history,summaryState.error,summary::retry)
                        }
                    }
                }
            }
        }
    }
}
