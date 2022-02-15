/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.fragments.home

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.View
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.common.ATHToolbarActivity
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.*
import code.name.monkey.retromusic.adapter.HomeAdapter
import code.name.monkey.retromusic.databinding.FragmentHomeBinding
import code.name.monkey.retromusic.dialogs.CreatePlaylistDialog
import code.name.monkey.retromusic.dialogs.ImportPlaylistDialog
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.drawNextToNavbar
import code.name.monkey.retromusic.extensions.elevatedAccentColor
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.glide.GlideApp
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.interfaces.IScrollHelper
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis

class HomeFragment :
    AbsMainActivityFragment(R.layout.fragment_home), IScrollHelper {

    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeBinding = FragmentHomeBinding.bind(view)
        _binding = HomeBinding(homeBinding)
        mainActivity.setSupportActionBar(binding.toolbar)
        mainActivity.supportActionBar?.title = null
        setupListeners()
//        binding.titleWelcome.text = String.format("%s", PreferenceUtil.userName)

        enterTransition = MaterialFadeThrough().addTarget(binding.contentContainer)
        reenterTransition = MaterialFadeThrough().addTarget(binding.contentContainer)

        val homeAdapter = HomeAdapter(mainActivity)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = homeAdapter
        }
        libraryViewModel.getHome().observe(viewLifecycleOwner) {
            Log.d("TAGGG", "onViewCreated: $it")
            homeAdapter.swapData(it)
        }
        libraryViewModel.getSuggestions().observe(viewLifecycleOwner) {
            loadSuggestions(it)
        }

        loadProfile()
        setupTitle()
        colorButtons()
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())
        binding.toolbar.drawNextToNavbar()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            remove()
            requireActivity().onBackPressed()
        }
        view.doOnLayout {
            adjustPlaylistButtons()
        }
    }

    private fun adjustPlaylistButtons() {
        val buttons =
            listOf(binding.actionShuffle)
//        buttons.maxOf { it.lineCount }.let { maxLineCount ->
//            buttons.forEach { button ->
//                // Set the highest line count to every button for consistency
//                button.setLines(maxLineCount)
//            }
//        }
    }

    private fun setupListeners() {
//        binding.bannerImage?.setOnClickListener {
//            findNavController().navigate(
//                R.id.user_info_fragment, null, null, FragmentNavigatorExtras(
//                    binding.userImage to "user_image"
//                )
//            )
//            reenterTransition = null
//        }

//        binding.lastAdded.setOnClickListener {
//            findNavController().navigate(
//                R.id.detailListFragment,
//                bundleOf(EXTRA_PLAYLIST_TYPE to LAST_ADDED_PLAYLIST)
//            )
//            setSharedAxisYTransitions()
//        }

//        binding.topPlayed.setOnClickListener {
//            findNavController().navigate(
//                R.id.detailListFragment,
//                bundleOf(EXTRA_PLAYLIST_TYPE to TOP_PLAYED_PLAYLIST)
//            )
//            setSharedAxisYTransitions()
//        }

        binding.actionShuffle.setOnClickListener {
            libraryViewModel.shuffleSongs()
        }

        binding.waveAnimation.setOnClickListener {
            binding.waveAnimation.toggleAnimation()
        }

        MusicPlayerRemote.pauseSong()

//        binding.history.setOnClickListener {
//            findNavController().navigate(
//                R.id.detailListFragment,
//                bundleOf(EXTRA_PLAYLIST_TYPE to HISTORY_PLAYLIST)
//            )
//            setSharedAxisYTransitions()
//        }

//        binding.userImage.setOnClickListener {
//            findNavController().navigate(
//                R.id.user_info_fragment, null, null, FragmentNavigatorExtras(
//                    binding.userImage to "user_image"
//                )
//            )
//        }
//        // Reload suggestions
//        binding.suggestions.refreshButton.setOnClickListener {
//            libraryViewModel.forceReload(
//                ReloadType.Suggestions
//            )
//        }
    }

    private fun setupTitle() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_search, null, navOptions)
        }
        val hexColor = String.format("#%06X", 0xFFFFFF and accentColor())
        val appName = HtmlCompat.fromHtml(
            "Unical <span  style='color:$hexColor';>Music</span>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        binding.appNameText.text = appName
    }

    private fun loadProfile() {
//        binding.bannerImage?.let {
//            GlideApp.with(requireContext())
//                .asBitmap()
//                .profileBannerOptions(RetroGlideExtension.getBannerModel())
//                .load(RetroGlideExtension.getBannerModel())
//                .into(it)
//        }
//        GlideApp.with(requireActivity()).asBitmap()
//            .userProfileOptions(RetroGlideExtension.getUserModel())
//            .load(RetroGlideExtension.getUserModel())
//            .into(binding.userImage)
    }

    fun colorButtons() {
//        binding.history.elevatedAccentColor()
//        binding.lastAdded.elevatedAccentColor()
//        binding.topPlayed.elevatedAccentColor()
        binding.actionShuffle.elevatedAccentColor(R.drawable.ic_play_arrow)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.findItem(R.id.action_settings).setShowAsAction(SHOW_AS_ACTION_IF_ROOM)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(binding.toolbar)
        )
        //Setting up cast button
        CastButtonFactory.setUpMediaRouteButton(requireContext(), menu, R.id.action_cast)
    }

    override fun scrollToTop() {
        binding.container.scrollTo(0, 0)
        binding.appBarLayout.setExpanded(true)
    }

    fun setSharedAxisXTransitions() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            addTarget(binding.root)
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    private fun setSharedAxisYTransitions() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
            addTarget(binding.root)
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    private fun loadSuggestions(songs: List<Song>) {
        if (songs.isEmpty()) {
//            binding.suggestions.root.isVisible = false
            return
        }
//        val images = listOf(
//            binding.suggestions.image1,
//            binding.suggestions.image2,
//            binding.suggestions.image3,
//            binding.suggestions.image4,
//            binding.suggestions.image5,
//            binding.suggestions.image6,
//            binding.suggestions.image7,
//            binding.suggestions.image8
//        )
        val color = ThemeStore.accentColor(requireContext())
//        binding.suggestions.message.apply {
//            setTextColor(color)
//            setOnClickListener {
//                it.isClickable = false
//                it.postDelayed({ it.isClickable = true }, 500)
//                MusicPlayerRemote.playNext(songs.subList(0, 8))
//                if (!MusicPlayerRemote.isPlaying) {
//                    MusicPlayerRemote.playNextSong()
//                }
//            }
//        }
//        binding.suggestions.card6.setCardBackgroundColor(ColorUtil.withAlpha(color, 0.12f))
//        images.forEachIndexed { index, imageView ->
//            imageView.setOnClickListener {
//                it.isClickable = false
//                it.postDelayed({ it.isClickable = true }, 500)
//                MusicPlayerRemote.playNext(songs[index])
//                if (!MusicPlayerRemote.isPlaying) {
//                    MusicPlayerRemote.playNextSong()
//                }
//            }
//            GlideApp.with(this)
//                .asBitmap()
//                .songCoverOptions(songs[index])
//                .load(RetroGlideExtension.getSongModel(songs[index]))
//                .into(imageView)
//        }
    }

    companion object {

        const val TAG: String = "BannerHomeFragment"

        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> findNavController().navigate(
                R.id.settingsActivity,
                null,
                navOptions
            )
            R.id.action_import_playlist -> ImportPlaylistDialog().show(
                childFragmentManager,
                "ImportPlaylist"
            )
            R.id.action_add_to_playlist -> CreatePlaylistDialog.create(emptyList()).show(
                childFragmentManager,
                "ShowCreatePlaylistDialog"
            )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), binding.toolbar)
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.HomeSections)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
