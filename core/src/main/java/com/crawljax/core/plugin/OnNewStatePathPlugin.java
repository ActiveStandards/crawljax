package com.crawljax.core.plugin;


import com.crawljax.core.CrawlerContext;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;

public interface OnNewStatePathPlugin extends Plugin {

    void onNewStatePath(StateVertex currentState, StateVertex newState, Eventable eventable, boolean targetIsNewState);

}
