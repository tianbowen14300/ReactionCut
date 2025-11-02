package com.tbw.cut.bilibili.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class BilibiliApiConstantsTest {

    @Test
    void testConstantsExist() {
        // Test that all constants are defined
        assertNotNull(BilibiliApiConstants.USER_INFO);
        assertNotNull(BilibiliApiConstants.VIDEO_VIEW);
        assertNotNull(BilibiliApiConstants.VIDEO_PLAY_URL);
        assertNotNull(BilibiliApiConstants.VIDEO_SEARCH);
        assertNotNull(BilibiliApiConstants.VIDEO_COMMENTS);
        assertNotNull(BilibiliApiConstants.VIDEO_DETAIL);
        assertNotNull(BilibiliApiConstants.VIDEO_DESCRIPTION);
        assertNotNull(BilibiliApiConstants.VIDEO_PAGE_LIST);
        assertNotNull(BilibiliApiConstants.LIVE_ROOM_INFO);
        assertNotNull(BilibiliApiConstants.LIVE_ROOM_GET_INFO);
        assertNotNull(BilibiliApiConstants.LIVE_ROOM_GET_INFO_OLD);
        assertNotNull(BilibiliApiConstants.LIVE_ROOM_INIT);
        assertNotNull(BilibiliApiConstants.LIVE_ROOM_BASE_INFO);
        assertNotNull(BilibiliApiConstants.LIVE_ROOM_STATUS_BATCH);
        assertNotNull(BilibiliApiConstants.LOGIN_WEB_KEY);
        assertNotNull(BilibiliApiConstants.LOGIN_WEB);
    }

    @Test
    void testConstantsValues() {
        // Test some specific constant values
        assertEquals("/x/space/acc/info", BilibiliApiConstants.USER_INFO);
        assertEquals("/x/web-interface/view", BilibiliApiConstants.VIDEO_VIEW);
        assertEquals("/x/player/playurl", BilibiliApiConstants.VIDEO_PLAY_URL);
        assertEquals("/x/live/web-room/v1/index/getInfoByRoom", BilibiliApiConstants.LIVE_ROOM_INFO);
        assertEquals("/x/passport-login/web/key", BilibiliApiConstants.LOGIN_WEB_KEY);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        // Test that the constructor is private
        Constructor<BilibiliApiConstants> constructor = BilibiliApiConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        
        // Test that instantiation throws AssertionError
        constructor.setAccessible(true);
        assertThrows(AssertionError.class, constructor::newInstance);
    }
}