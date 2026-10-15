package io.github.thomaswcode.notificationvibrator.ui.apps

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.thomaswcode.notificationvibrator.RoutingSnapshot
import io.github.thomaswcode.notificationvibrator.appContainer
import io.github.thomaswcode.notificationvibrator.ui.components.AppInfoCache
import io.github.thomaswcode.notificationvibrator.vibration.PatternLibrary
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppRow(
    val packageName: String,
    val label: String,
    val isAssigned: Boolean,
    val pattern: VibrationPattern,
    val intensity: Float,
)

data class AppsUiState(
    val loadingApps: Boolean = true,
    val defaultPattern: VibrationPattern = PatternLibrary.default,
    val defaultIntensity: Float = 1f,
    val assigned: List<AppRow> = emptyList(),
    val recent: List<AppRow> = emptyList(),
    val all: List<AppRow> = emptyList(),
)

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val pm = application.packageManager
    private val launchablePackages = MutableStateFlow<List<String>?>(null)

    val query = MutableStateFlow("")

    val patterns: StateFlow<List<VibrationPattern>> =
        container.patternRepository.patterns.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatternLibrary.all)

    val state: StateFlow<AppsUiState> = combine(
        launchablePackages,
        container.activityRepository.seenApps,
        container.snapshot.filterNotNull(),
        query,
    ) { packages, seen, snapshot, query ->
        fun matches(row: AppRow) =
            query.isBlank() || row.label.contains(query, ignoreCase = true) || row.packageName.contains(query, ignoreCase = true)
        val byLabel = compareBy<AppRow> { it.label.lowercase() }
        AppsUiState(
            loadingApps = packages == null,
            defaultPattern = snapshot.defaultPattern,
            defaultIntensity = snapshot.settings.defaultIntensity,
            assigned = snapshot.assignments.keys.map { row(it, snapshot) }.filter(::matches).sortedWith(byLabel),
            recent = seen.take(RECENT_LIMIT).map { row(it.packageName, snapshot) }.filter(::matches),
            all = packages.orEmpty().map { row(it, snapshot) }.filter(::matches).sortedWith(byLabel),
        )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) { launchablePackages.value = loadLaunchablePackages() }
    }

    fun assign(packageName: String, patternId: String, intensity: Float) {
        container.appScope.launch { container.assignmentRepository.assign(packageName, patternId, intensity) }
    }

    fun reset(packageName: String) {
        container.appScope.launch { container.assignmentRepository.reset(packageName) }
    }

    fun setDefault(patternId: String, intensity: Float) {
        container.appScope.launch { container.settingsStore.setDefaultPattern(patternId, intensity) }
    }

    fun preview(pattern: VibrationPattern, intensity: Float) = container.preview(pattern, intensity)

    private fun row(packageName: String, snapshot: RoutingSnapshot) = AppRow(
        packageName = packageName,
        label = AppInfoCache.label(pm, packageName),
        isAssigned = packageName in snapshot.assignments,
        pattern = snapshot.patternFor(packageName),
        intensity = snapshot.intensityFor(packageName),
    )

    private fun loadLaunchablePackages(): List<String> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val ownPackage = getApplication<Application>().packageName
        return pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != ownPackage }
            .onEach { AppInfoCache.label(pm, it) }
    }

    private companion object {
        const val RECENT_LIMIT = 15
    }
}
