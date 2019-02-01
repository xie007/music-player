package com.github.anrimian.musicplayer;

public interface Constants {

    int NO_POSITION = -1;

    interface Arguments {
        String ORDER_ARG = "order_arg";
        String PLAY_LIST_ID_ARG = "play_list_id_arg";
        String PATH_ARG = "path_arg";
        String STATUS_BAR_COLOR_ATTR_ARG = "status_bar_color_attr";
    }

    interface Tags {
        String ORDER_TAG = "order_tag";
        String SELECT_PLAYLIST_TAG = "select_playlist_tag";
        String SELECT_PLAYLIST_FOR_FOLDER_TAG = "select_playlist_for_folder_tag";
        String CREATE_PLAYLIST_TAG = "create_playlist_tag";
        String PLAY_LIST_MENU = "play_list_menu";
    }

    interface Animation {
        int TOOLBAR_ARROW_ANIMATION_TIME = 200;
    }
}
