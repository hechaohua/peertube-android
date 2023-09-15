/*
 * Copyright (C) 2020 Stefan Schüller <sschueller@techdroid.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.schueller.peertube.fragment

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.Iconics
import com.driverolder.R
import net.schueller.peertube.adapter.MultiViewRecycleViewAdapter
import net.schueller.peertube.database.VideoViewModel
import net.schueller.peertube.helper.APIUrlHelper
import net.schueller.peertube.helper.ErrorHelper
import net.schueller.peertube.model.CommentThread
import net.schueller.peertube.model.Rating
import net.schueller.peertube.model.Video
import net.schueller.peertube.model.VideoList
import net.schueller.peertube.model.ui.VideoMetaViewItem
import net.schueller.peertube.network.GetVideoDataService
import net.schueller.peertube.network.RetrofitInstance
import com.driverolder.service.VideoPlayerService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VideoMetaDataFragment : Fragment() {
    private var videoRating: Rating? = null
    private var defaultTextColor: ColorStateList? = null
    private var recyclerView: RecyclerView? = null
    private var mMultiViewAdapter: MultiViewRecycleViewAdapter? = null

    private lateinit var videoDescriptionFragment: VideoDescriptionFragment

    private val mVideoViewModel: VideoViewModel by activityViewModels()

    var isLeaveAppExpected = false
        private set

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video_meta, container, false)
    }

    override fun onPause() {
        isLeaveAppExpected = false
        super.onPause()
    }

    fun showDescriptionFragment(video: Video) {
        // show full description fragment
        videoDescriptionFragment = VideoDescriptionFragment.newInstance(video, this)
        childFragmentManager.beginTransaction()
                .add(R.id.video_meta_data_fragment, videoDescriptionFragment, VideoDescriptionFragment.TAG).commit()
    }

    fun hideDescriptionFragment() {
        val fragment: Fragment? = childFragmentManager.findFragmentByTag(VideoDescriptionFragment.TAG)
        if (fragment != null) {
            childFragmentManager.beginTransaction().remove(fragment).commit()
        }
    }

    fun updateVideoMeta(video: Video, mService: VideoPlayerService?) {

        // Remove description if it is open as we are loading a new video
        hideDescriptionFragment()

        val context = context
        val activity: Activity? = activity
        val apiBaseURL = APIUrlHelper.getUrlWithVersion(context)
        val videoDataService = RetrofitInstance.getRetrofitInstance(
                apiBaseURL,
                APIUrlHelper.useInsecureConnection(context)
        ).create(
                GetVideoDataService::class.java
        )

        // related videos
        recyclerView = activity!!.findViewById(R.id.relatedVideosView)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this@VideoMetaDataFragment.context)
        recyclerView?.layoutManager = layoutManager
        mMultiViewAdapter = MultiViewRecycleViewAdapter(this)
        recyclerView?.adapter = mMultiViewAdapter

        val videoMetaViewItem = VideoMetaViewItem()
        videoMetaViewItem.video = video
        mMultiViewAdapter?.setVideoMeta(videoMetaViewItem)

        loadVideos()

//        loadComments(video.id)

//        mMultiViewAdapter?.setVideoComment()

        // videoOwnerSubscribeButton


        // description


        // video player options
        val videoOptions = activity.findViewById<TextView>(R.id.exo_more)
        videoOptions.setText(R.string.video_more_icon)
        Iconics.Builder().on(videoOptions).build()
        videoOptions.setOnClickListener {
            val videoOptionsFragment = VideoOptionsFragment.newInstance(mService, video.files)
            videoOptionsFragment.show(
                    getActivity()!!.supportFragmentManager,
                    VideoOptionsFragment.TAG
            )
        }
    }

    private fun loadComments(videoId: Int) {
        val context = context

        val start = 0
        val count = 1
        val sort = "-createdAt"


        // We set this to default to null so that on initial start there are videos listed.
        val apiBaseURL = APIUrlHelper.getUrlWithVersion(context)
        val service =
                RetrofitInstance.getRetrofitInstance(apiBaseURL, APIUrlHelper.useInsecureConnection(context)).create(
                        GetVideoDataService::class.java
                )
        val call: Call<CommentThread> = service.getCommentThreads(videoId, start, count, sort)

        call.enqueue(object : Callback<CommentThread?> {
            override fun onResponse(call: Call<CommentThread?>, response: Response<CommentThread?>) {
                if (response.body() != null) {
                    val commentThread = response.body()
                    if (commentThread != null) {
                        mMultiViewAdapter!!.setVideoComment(commentThread)
                    }
                }
            }

            override fun onFailure(call: Call<CommentThread?>, t: Throwable) {
                Log.wtf("err", t.fillInStackTrace())
                ErrorHelper.showToastFromCommunicationError(this@VideoMetaDataFragment.context, t)
            }
        })
    }

    private fun loadVideos() {
        val context = context

        val start = 0
        val count = 6
        val sort = "-createdAt"
        val filter: String? = null

        val sharedPref = context?.getSharedPreferences(
                context.packageName + "_preferences",
                Context.MODE_PRIVATE
        )

        var nsfw = "false"
        var languages: Set<String>? = emptySet()
        if (sharedPref != null) {
            nsfw = if (sharedPref.getBoolean(getString(R.string.pref_show_nsfw_key), false)) "both" else "false"
            languages = sharedPref.getStringSet(getString(R.string.pref_video_language_key), null)
        }

        // We set this to default to null so that on initial start there are videos listed.
        val apiBaseURL = APIUrlHelper.getUrlWithVersion(context)
        val service =
                RetrofitInstance.getRetrofitInstance(apiBaseURL, APIUrlHelper.useInsecureConnection(context)).create(
                        GetVideoDataService::class.java
                )
        val call: Call<VideoList> = service.getVideosData(start, count, sort, nsfw, filter, languages)

        /*Log the URL called*/Log.d("URL Called", call.request().url.toString() + "")
        //        Toast.makeText(VideoListActivity.this, "URL Called: " + call.request().url(), Toast.LENGTH_SHORT).show();
        call.enqueue(object : Callback<VideoList?> {
            override fun onResponse(call: Call<VideoList?>, response: Response<VideoList?>) {
                if (response.body() != null) {
                    val videoList = response.body()
                    if (videoList != null) {
                        mMultiViewAdapter!!.setVideoListData(videoList)
                    }
                }
            }

            override fun onFailure(call: Call<VideoList?>, t: Throwable) {
                Log.wtf("err", t.fillInStackTrace())
                ErrorHelper.showToastFromCommunicationError(this@VideoMetaDataFragment.context, t)
            }
        })
    }

    fun saveToPlaylist(video: Video) {
        val playlistVideo: net.schueller.peertube.database.Video = net.schueller.peertube.database.Video(videoUUID = video.uuid, videoName = video.name, videoDescription = video.description)
        mVideoViewModel.insert(playlistVideo)
    }

    companion object {
        const val TAG = "VMDF"
    }
}