package com.example.libvlc_custom.player.observables

import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.RendererDiscoverer
import org.videolan.libvlc.RendererItem
import java.util.ArrayList
import java.util.Observable

import org.videolan.libvlc.RendererDiscoverer.Event.ItemAdded
import org.videolan.libvlc.RendererDiscoverer.Event.ItemDeleted


class RendererItemObservable(private val libVlc: LibVLC) : Observable(),
    RendererDiscoverer.EventListener {

    private val rendererItems = ArrayList<RendererItem>()
    private val rendererDiscoverers = ArrayList<RendererDiscoverer>()

    /**
     * Start listening for renderer item events.
     */
    fun start() {
        for (discoverer in RendererDiscoverer.list(libVlc)) {
            val rendererDiscoverer = RendererDiscoverer(
                libVlc,
                discoverer.name
            )
            rendererDiscoverers.add(rendererDiscoverer)
            rendererDiscoverer.setEventListener(this)
            rendererDiscoverer.start()
        }
    }

    /**
     * Stop listening for renderer item events.
     */
    fun stop() {
        for (discover in rendererDiscoverers) {
            discover.stop()
        }
        for (renderItem in rendererItems) {
            renderItem.release()
        }
        rendererDiscoverers.clear()
        rendererItems.clear()
    }

    /**
     * Get all of the renderer items currently available.
     *
     * @return A list of all of the renderer items available.
     */
    fun getRenderItems(): List<RendererItem?> {
        return rendererItems
    }


    override fun onEvent(event: RendererDiscoverer.Event?) {
        event?.let {
            when (it.type) {
                ItemAdded -> {
                    onItemAdded(event.item)
                }
                ItemDeleted -> {
                    onItemDeleted(event.item)
                }
                else -> {

                }
            }
        }
    }

    /**
     * Remove renderer item and notify subscribers.
     *
     * @param item The item to remove.
     */
    private fun onItemDeleted(item: RendererItem) {
        rendererItems.remove(item)
        setChanged()
        notifyObservers(rendererItems)
    }

    /**
     * Add renderer item and notify subscribers.
     *
     * @param item The item to add.
     */
    private fun onItemAdded(item: RendererItem) {
        rendererItems.add(item)
        setChanged()
        notifyObservers(rendererItems)
    }
}