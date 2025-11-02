package com.tbw.cut.bilibili.service;

import com.tbw.cut.bilibili.service.BilibiliQRCodeLoginService.QRCodeLoginResult;
import com.tbw.cut.bilibili.service.BilibiliQRCodeLoginService.PollResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BilibiliQRCodeLoginServiceTest {

    @Test
    void contextLoads() {
        // This test ensures the context loads properly
        // Actual Bilibili QR code login tests would require network access
        // and would be integration tests rather than unit tests
    }

    @Test
    void testQRCodeLoginResultClass() {
        QRCodeLoginResult result = new QRCodeLoginResult(true, "success", "cookies");
        
        assertTrue(result.isSuccess());
        assertEquals("success", result.getMessage());
        assertEquals("cookies", result.getCookies());
    }
    
    @Test
    void testPollResultClass() {
        PollResult result = new PollResult(0, "success", null);
        
        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    // The following tests are commented out because they require the full Spring context
    // and actual service beans to be injected
    
    /*
    @Autowired
    private BilibiliQRCodeLoginService bilibiliQRCodeLoginService;
    */
}