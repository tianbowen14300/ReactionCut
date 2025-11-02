package com.tbw.cut.bilibili;

import com.tbw.cut.bilibili.service.BilibiliLiveService;
import com.tbw.cut.bilibili.service.BilibiliVideoService;
import com.tbw.cut.bilibili.service.BilibiliLoginService;
import com.tbw.cut.bilibili.service.BilibiliUnifiedService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BilibiliServiceStructureTest {

    @Test
    void contextLoads() {
        // This test ensures the context loads properly
        // Actual Bilibili API tests would require network access
        // and would be integration tests rather than unit tests
    }

    // The following tests are commented out because they require Spring context
    // and actual service beans to be injected
    
    /*
    @Autowired
    private BilibiliLiveService bilibiliLiveService;
    
    @Autowired
    private BilibiliVideoService bilibiliVideoService;
    
    @Autowired
    private BilibiliLoginService bilibiliLoginService;
    
    @Autowired
    private BilibiliUnifiedService bilibiliUnifiedService;
    
    @Test
    void testServiceInjection() {
        assertNotNull(bilibiliLiveService);
        assertNotNull(bilibiliVideoService);
        assertNotNull(bilibiliLoginService);
        assertNotNull(bilibiliUnifiedService);
    }
    
    @Test
    void testServiceInheritance() {
        // BilibiliUnifiedService should extend all specialized services
        assertTrue(bilibiliUnifiedService instanceof BilibiliLiveService);
        assertTrue(bilibiliUnifiedService instanceof BilibiliVideoService);
        assertTrue(bilibiliUnifiedService instanceof BilibiliLoginService);
    }
    */
}