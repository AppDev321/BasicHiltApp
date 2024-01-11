package com.core.extensions

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    when {
        currentFocus != null -> currentFocus
        getActivityRoot() != null -> getActivityRoot()
        else -> View(this)
    }?.let { focusedView ->
        hideKeyboard(focusedView)
    }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.showKeyboard() {
    view?.let { activity?.showKeyboard() }
}

fun Activity.showKeyboard() {
    showKeyboard(this)
}

fun showKeyboard(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun showKeyboard(editText: EditText) {
    editText.requestFocus()
    val inputMethodManager =
        editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
}

fun hideSoftKeyboard(editText: EditText) {
    val imm =
        editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(editText.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
}

@RequiresApi(Build.VERSION_CODES.R)
fun keyboardVisible(activity: Activity): Boolean {
    return activity.getActivityRoot()?.rootWindowInsets?.isVisible(
        WindowInsets.Type.ime()
    ) ?: false
}

fun Activity.getActivityRoot(): View? {
    return (findViewById<ViewGroup>(android.R.id.content)).getChildAt(0);
}

fun Fragment.hideKeyboardAndClearFocus(editText: EditText?) {
    val view = activity?.currentFocus
    if (view != null) {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        editText?.clearFocus()
    } else {
        if (editText != null) {
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
            editText.clearFocus()
        }
    }
}
