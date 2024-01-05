package com.dopsi.webapp.fragment

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import com.core.base.BaseFragment
import com.core.utils.PermissionUtilsNew
import com.core.utils.Utils
import com.core.utils.fileUtils.FileUtils
import com.dopsi.webapp.databinding.FragmentWebViewBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream

class WebViewFragment :BaseFragment<FragmentWebViewBinding>(FragmentWebViewBinding::inflate){


    private val URL = "https://mis.dopasi.org/transportation/"
    private var isAlreadyCreated = false
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var resultLauncher: ActivityResultLauncher<Intent>
    private var permissionsDeniedPermanently = false
    private lateinit var imageFile : File

    init {
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                onActivityResult(result.resultCode, result.data)
            }
    }

    override fun initUserInterface(view: View?) {
        imageFile = FileUtils.getTakePhotoFile(requireActivity())
        setupWebView()
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    viewDataBinding.apply {
                        if ( webView.canGoBack()) {
                            webView.goBack()
                        }
                        else
                        {
                            activity?.onBackPressed()
                        }
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }
    override fun onResume() {
        super.onResume()

        if (isAlreadyCreated && !isNetworkAvailable()) {
            isAlreadyCreated = false
            showErrorDialog(
                "Error", "No internet connection. Please check your connection.",
                requireActivity()
            )
        }
    }

    private fun setupWebView() {
        startLoaderAnimate()
        viewDataBinding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    endLoaderAnimate()
                }


                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    endLoaderAnimate()
                    showErrorDialog(
                        "Error",
                        "No internet connection. Please check your connection.",
                        requireContext()
                    )
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    fileChooserCallback = filePathCallback
                    checkAndShowImagePicker()

                    return true
                }

            }

            loadUrl(URL)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    }
/*

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        viewDataBinding.apply {
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

*/


    private fun showErrorDialog(title: String, message: String, context: Context) {
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle(title)
        dialog.setMessage(message)
        dialog.setNegativeButton("Cancel") { _, _ ->
           activity?.finish()
        }
        dialog.setNeutralButton("Settings") { _, _ ->
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        dialog.setPositiveButton("Retry") { _, _ ->
            setupWebView()
        }
        dialog.create().show()
    }

    private fun endLoaderAnimate() {
        viewDataBinding.apply {
            loaderImage.clearAnimation()
            loaderImage.visibility = View.GONE
        }
    }

    private fun startLoaderAnimate() {
        val objectAnimator = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val startHeight = 170
                val newHeight = (startHeight + (startHeight + 40) * interpolatedTime).toInt()
                viewDataBinding.apply {
                    loaderImage.layoutParams.height = newHeight
                    loaderImage.requestLayout()
                }
            }

            override fun initialize(width: Int, height: Int, parentWidth: Int, parentHeight: Int) {
                super.initialize(width, height, parentWidth, parentHeight)
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        objectAnimator.repeatCount = -1
        objectAnimator.repeatMode = ValueAnimator.REVERSE
        objectAnimator.duration = 1000
        viewDataBinding.apply {
            loaderImage.startAnimation(objectAnimator)
        }
    }

    private fun onActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (fileChooserCallback != null) {
                val result = if (data == null || data.data == null) {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                   // val uri = saveImageAndGetUri(imageBitmap)
                   // arrayOf(uri)
                    arrayOf(Uri.fromFile(imageFile))
                } else {
                    // Gallery result
                    arrayOf(data.data!!)
                }
                fileChooserCallback!!.onReceiveValue(result)
                fileChooserCallback = null
            }
        } else {
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = null
        }
    }


    fun checkAndShowImagePicker() {
        val list: MutableList<String> = ArrayList()
        if (Utils.androidTIRAMISUAndAbove.not()) {
            if (Utils.androidRAndAbove) {
                list.add(PermissionUtilsNew.getReadStoragePermissionString())
            } else if (Utils.androidRAndAbove.not())
                list.add(PermissionUtilsNew.getWriteStoragePermissionString())
        }
        list.add(Manifest.permission.CAMERA)
        checkPermissionAndMakeAction(list)

    }

    private fun checkPermissionAndMakeAction(permissions: List<String>) {
        if (permissions.isEmpty()) {
            performAttachAction(
                areAllPermissionsGranted = true,
                isAnyPermissionPermanentlyDenied = false
            )
            return
        }
        checkPermissionDeniedPermanently(permissions)
        Dexter.withContext(requireContext())
            .withPermissions(permissions).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    performAttachAction(
                        multiplePermissionsReport.areAllPermissionsGranted(),
                        multiplePermissionsReport.isAnyPermissionPermanentlyDenied
                    )
                }

                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest>,
                    permissionToken: PermissionToken,
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).check()
    }

    private fun performAttachAction(
        areAllPermissionsGranted: Boolean,
        isAnyPermissionPermanentlyDenied: Boolean
    ) {
        if (areAllPermissionsGranted) {

            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val chooserIntent = Intent.createChooser(galleryIntent, "Choose Image Source")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            chooserIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFile)
            resultLauncher.launch(chooserIntent)

        } else if (isAnyPermissionPermanentlyDenied) {
            if (permissionsDeniedPermanently)
                PermissionUtilsNew.showPermissionSettingsDialog(requireActivity())
            else
                permissionsDeniedPermanently = true
        }

    }

    private fun checkPermissionDeniedPermanently(permissions: List<String>) {
        when (permissions.size) {
            2 -> {
                permissionsDeniedPermanently =
                    !shouldShowRequestPermissionRationale(permissions[0])
                            || !shouldShowRequestPermissionRationale(permissions[1])
                permissionsDeniedPermanently = shouldShowRequestPermissionRationale(
                    permissions[0]
                ) == false
            }

            1 -> permissionsDeniedPermanently = shouldShowRequestPermissionRationale(
                permissions[0]
            ) == false
        }

    }

    private fun saveImageAndGetUri(bitmap: Bitmap): Uri {
       val imageFile= FileUtils.getTakePhotoFile(requireActivity())
        val fileOutputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        return Uri.fromFile(imageFile)
    }
}