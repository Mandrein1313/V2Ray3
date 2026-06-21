package com.v2ray.ang.ui

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.tbruyelle.rxpermissions2.RxPermissions // ใช้ rxpermissions2 สำหรับ AndroidX
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.FragmentRoutingSettingsBinding
import com.v2ray.ang.extension.defaultDPreference
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

class RoutingSettingsFragment : Fragment() {
    private var _binding: FragmentRoutingSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val routing_arg = "routing_arg"
        private const val REQUEST_SCAN_REPLACE = 11
        private const val REQUEST_SCAN_APPEND = 12
    }

    fun newInstance(arg: String): Fragment {
        val fragment = RoutingSettingsFragment()
        val bundle = Bundle()
        bundle.putString(routing_arg, arg)
        fragment.arguments = bundle
        return fragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRoutingSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val content = activity?.defaultDPreference?.getPrefString(arguments?.getString(routing_arg) ?: "", "")
        binding.etRoutingContent.text = Utils.getEditable(content ?: "")

        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_routing, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.save_routing -> {
            val content = binding.etRoutingContent.text.toString()
            activity?.defaultDPreference?.setPrefString(arguments?.getString(routing_arg) ?: "", content)
            activity?.toast(R.string.toast_success)
            true
        }
        R.id.del_routing -> {
            binding.etRoutingContent.text = null
            true
        }
        R.id.scan_replace -> {
            scanQRcode(REQUEST_SCAN_REPLACE)
            true
        }
        R.id.scan_append -> {
            scanQRcode(REQUEST_SCAN_APPEND)
            true
        }
        R.id.default_rules -> {
            setDefaultRules()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun scanQRcode(requestCode: Int): Boolean {
        activity?.let {
            RxPermissions(this)
                .request(Manifest.permission.CAMERA)
                .subscribe { granted ->
                    if (granted)
                        startActivityForResult(Intent(it, ScannerActivity::class.java), requestCode)
                    else
                        it.toast(R.string.toast_permission_denied)
                }
        }
        return true
    }

    private fun setDefaultRules(): Boolean {
        var url = AppConfig.v2rayCustomRoutingListUrl
        when (arguments?.getString(routing_arg)) {
            AppConfig.PREF_V2RAY_ROUTING_AGENT -> url += AppConfig.TAG_AGENT
            AppConfig.PREF_V2RAY_ROUTING_DIRECT -> url += AppConfig.TAG_DIRECT
            AppConfig.PREF_V2RAY_ROUTING_BLOCKED -> url += AppConfig.TAG_BLOCKED
        }

        activity?.toast(R.string.msg_downloading_content)
        GlobalScope.launch(Dispatchers.IO) {
            val content = try { URL(url).readText() } catch (e: Exception) { "" }
            launch(Dispatchers.Main) {
                binding.etRoutingContent.text = Utils.getEditable(content)
                activity?.toast(R.string.toast_success)
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val result = data?.getStringExtra("SCAN_RESULT") ?: ""
            when (requestCode) {
                REQUEST_SCAN_REPLACE -> binding.etRoutingContent.text = Utils.getEditable(result)
                REQUEST_SCAN_APPEND -> binding.etRoutingContent.text = Utils.getEditable("${binding.etRoutingContent.text},$result")
            }
        }
    }
}
