package com.sequenceiq.cloudbreak.service.image;

import static org.mockito.BDDMockito.given;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class HdpInfoSearchServiceTest {

    private CloudbreakImageCatalog cloudbreakImageCatalog;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @InjectMocks
    private HdpInfoSearchService underTest;

    @Before
    public void setup() throws IOException {
        String catalogJson = FileReaderUtils.readFileFromClasspath("image/cb-image-catalog.json");
        cloudbreakImageCatalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalog.class);

        given(imageCatalogProvider.getImageCatalog()).willReturn(cloudbreakImageCatalog);
    }

    @Test
    public void testWithNull() throws IOException {
        HDPInfo hdpInfo = underTest.searchHDPInfo(null, null);
        Assert.assertNull("HDP info shall be null for null input", hdpInfo);
    }

    @Test
    public void testWithNotExsisting() {
        HDPInfo hdpInfo = underTest.searchHDPInfo("2.4.0.0-661", "2.4.3.0-21");
        Assert.assertNull("HDP info shall be null for non exsisting combination", hdpInfo);

        hdpInfo = underTest.searchHDPInfo("2.4.0.0-660", "2.4.3.0-14");
        Assert.assertNull("HDP info shall be null for non exsisting combination", hdpInfo);
    }

    @Test
    public void testExactVersion() {
        HDPInfo hdpInfo = underTest.searchHDPInfo("2.4.0.0-660", "2.4.3.0-21");
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.searchHDPInfo("2.5.0.0-222", "2.5.0.0-723");
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testPrefix() {
        HDPInfo hdpInfo = underTest.searchHDPInfo("2.4", "2.4");
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-770-2.4.10.0-100", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-770-2.4.10.0-100", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

}