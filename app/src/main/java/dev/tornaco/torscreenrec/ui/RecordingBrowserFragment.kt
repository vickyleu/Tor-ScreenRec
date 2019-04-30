package dev.tornaco.torscreenrec.ui

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.loader.VideoProvider
import dev.tornaco.torscreenrec.modle.Video
import dev.tornaco.torscreenrec.util.MediaTools
import dev.tornaco.torscreenrec.util.ThreadUtil
import org.newstand.logger.Logger
import java.io.File
import java.util.*


/**
 * Created by Tornaco on 2017/7/27.
 * Licensed with Apache.
 */

class RecordingBrowserFragment : Fragment() {

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: Adapter? = null

    protected val layoutManager: RecyclerView.LayoutManager
        get() = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.recycler_view_template, container, false)
        setupView(root)
        return root
    }

    fun setupView(root: View) {

        swipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout!!.setColorSchemeColors(*resources.getIntArray(R.array.polluted_waves))

        mRecyclerView = root.findViewById(R.id.recycler_view)

        swipeRefreshLayout!!.setOnRefreshListener { startLoading() }

        setupAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.setTitle(R.string.title_recording_browser)
        showRetention()
    }

    private fun startLoading() {
        swipeRefreshLayout!!.isRefreshing = true
        ThreadUtil.newThread(Runnable {
            val videos = VideoProvider(context).list

            ThreadUtil.mainThreadHandler.post(Runnable {
                mAdapter!!.update(videos)
                swipeRefreshLayout!!.isRefreshing = false
            })
        }).run()
    }

    private fun requestPerms() {
        val rxPermissions = RxPermissions(activity!!)
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted!!) {
                        onPermissionGrant()
                    } else {
                        onPermissionNotGrant()
                    }
                }.toString()
    }

    private fun onPermissionNotGrant() {
        activity!!.finish()
    }

    private fun onPermissionGrant() {
        startLoading()
    }

    private fun showRetention() {
        val hasBasicPermission = ContextCompat.checkSelfPermission(context!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasBasicPermission) {
            MaterialStyledDialog.Builder(activity!!)
                    .setTitle(R.string.title_perm_require)
                    .setDescription(R.string.summary_perm_require)
                    .setIcon(R.drawable.ic_folder_white_24dp)
                    .withDarkerOverlay(false)
                    .setCancelable(false)
                    .setPositiveText(android.R.string.ok)
                    .setNegativeText(android.R.string.cancel)
                    .onPositive { dialog, which -> requestPerms() }
                    .onNegative { dialog, which -> activity!!.finish() }
                    .show()
        } else {
            onPermissionGrant()
        }
    }

    protected fun setupAdapter() {
        mRecyclerView!!.setHasFixedSize(true)
        setupLayoutManager()
        mAdapter = Adapter()
        mRecyclerView!!.adapter = mAdapter

    }

    protected fun setupLayoutManager() {
        mRecyclerView!!.layoutManager = layoutManager
    }


    internal inner class TwoLinesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var title: TextView
        var description: TextView
        var thumbnail: ImageView

        init {
            title = itemView.findViewById<View>(android.R.id.title) as TextView
            description = itemView.findViewById<View>(android.R.id.text1) as TextView
            thumbnail = itemView.findViewById<View>(R.id.avatar) as ImageView
        }
    }


    private inner class Adapter @JvmOverloads constructor(private val data: MutableList<Video> = ArrayList()) : RecyclerView.Adapter<TwoLinesViewHolder>() {

        fun update(data: List<Video>) {
            this.data.clear()
            this.data.addAll(data)
            notifyDataSetChanged()
        }

        fun remove(position: Int) {
            this.data.removeAt(position)
            notifyItemRemoved(position)
        }

        fun add(video: Video, position: Int) {
            this.data.add(position, video)
            notifyItemInserted(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TwoLinesViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.simple_card_item, parent, false)
            return TwoLinesViewHolder(view)
        }

        override fun onBindViewHolder(holder: TwoLinesViewHolder, position: Int) {
            val item = data[position]
            holder.title.text = item.title
            val descriptionText = item.duration
            holder.description.text = descriptionText
            holder.itemView.setOnClickListener {
                val popupMenu = PopupMenu(activity!!, holder.description)
                popupMenu.inflate(R.menu.actions)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_play -> startActivity(MediaTools.buildOpenIntent(requireContext(),
                                File(item.path)))
                        R.id.action_remove -> ThreadUtil.workThreadHandler.post(Runnable {
                            File(item.path).delete()
                            remove(holder.adapterPosition)
                        })
                        R.id.action_rename -> ThreadUtil.mainThreadHandler.post(Runnable { showRenameDialog(item.title, item.path) })
                        R.id.action_share -> startActivity(MediaTools.buildSharedIntent(requireContext(),
                                File(item.path)))
                        R.id.action_togif -> {
                            val path = item.path
                            toGif(path, File(path).parent + File.separator + getNameWithoutExtension(path) + ".gif")
                        }
                    }
                    true
                }
                popupMenu.show()
            }
            Glide.with(context!!).load(item.path).into(holder.thumbnail)
        }

        override fun getItemCount(): Int {
            return data.size
        }


        internal fun showRenameDialog(hint: String?, fromPath: String?) {
            val editTextContainer = LayoutInflater.from(context).inflate(dev.nick.tiles.R.layout.dialog_edit_text, null, false)
            val editText = editTextContainer.findViewById<View>(dev.nick.tiles.R.id.edit_text) as EditText
            editText.hint = hint
            val alertDialog = AlertDialog.Builder(context!!)
                    .setView(editTextContainer)
                    .setTitle(R.string.action_rename)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        ThreadUtil.workThreadHandler.post(Runnable {
                            val parent = File(fromPath).parentFile
                            val to = File(parent, editText.text.toString() + ".mp4")
                            File(fromPath).renameTo(to)
                            MediaScannerConnection.scanFile(context,
                                    arrayOf(to.absolutePath), null
                            ) { path, uri -> startLoading() }
                        })
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
            alertDialog.show()
        }

        fun getNameWithoutExtension(file: String?): String {
            val fileName = File(file).name
            val dotIndex = fileName.lastIndexOf('.')
            return if (dotIndex == -1) fileName else fileName.substring(0, dotIndex)
        }

        internal fun toGif(path: String?, dest: String) {
            val command = String.format("-y -i %s -pix_fmt rgb24 -r 10 %s", path, dest)
            Logger.d("Command:$command")
            val commands = command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val p = ProgressDialog(context)
            p.setTitle(R.string.action_togif)
            p.setCancelable(false)
            p.isIndeterminate = true

            //            try {
            //                FFmpeg.getInstance(getContext()).execute(commands,
            //                        new FFmpegExecuteResponseHandler() {
            //                    @Override
            //                    public void onSuccess(String message) {
            //                        Logger.d(message);
            //                        Snackbar.make(mRecyclerView, getString(R.string.result_to_gif_ok, dest),
            //                                Snackbar.LENGTH_INDEFINITE)
            //                                .setAction(android.R.string.ok, new View.OnClickListener() {
            //                                    @Override
            //                                    public void onClick(View v) {
            //
            //                                    }
            //                                })
            //                                .show();
            //                    }
            //
            //                    @Override
            //                    public void onProgress(String message) {
            //                        p.setMessage(message);
            //                        Logger.d(message);
            //                    }
            //
            //                    @Override
            //                    public void onFailure(final String message) {
            //                        Logger.d(message);
            //                        Snackbar.make(mRecyclerView, getString(R.string.result_to_gif_fail), Snackbar.LENGTH_INDEFINITE)
            //                                .setAction(android.R.string.ok, new View.OnClickListener() {
            //                                    @Override
            //                                    public void onClick(View v) {
            //                                        new AlertDialog.Builder(getContext())
            //                                                .setMessage(message)
            //                                                .setTitle(R.string.result_to_gif_fail)
            //                                                .setCancelable(false)
            //                                                .setPositiveButton(android.R.string.ok, null)
            //                                                .create()
            //                                                .show();
            //                                    }
            //                                })
            //                                .show();
            //                    }
            //
            //                    @Override
            //                    public void onStart() {
            //                        p.show();
            //                    }
            //
            //                    @Override
            //                    public void onFinish() {
            //                        p.dismiss();
            //                    }
            //                });
            //            } catch (FFmpegCommandAlreadyRunningException e) {
            //                Toast.makeText(getContext(), "FFmpegCommandAlreadyRunningException", Toast.LENGTH_LONG).show();
            //            }
        }
    }
}
