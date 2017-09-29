package lassie.awshandlers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.TagSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class S3HandlerIT {
    @Mock
    private AmazonS3 s3;
    @InjectMocks
    private S3HandlerImpl s3Handler;

    @Test
    public void bucketHasTagOwnerReturnsTrue() throws Exception {
        // given
        String bucket = "bucket";
        Tag tag = new Tag("Owner", "John Doe");

        List<TagSet> tagSets = new ArrayList<>();
        TagSet tagSet = new TagSet();
        tagSet.setTag(tag.getKey(), tag.getValue());
        tagSets.add(tagSet);

        BucketTaggingConfiguration configuration = new BucketTaggingConfiguration();
        configuration.setTagSets(tagSets);

        // when
        when(s3.getBucketTaggingConfiguration(bucket))
                .thenReturn(configuration);

        boolean result = s3Handler.bucketHasTag(bucket, "Owner");

        // then
        assertEquals(true, result);

    }

    @Test
    public void bucketHasTagCreatorReturnsFalse() throws Exception {
        // given
        String bucket = "bucket";
        Tag tag = new Tag("Owner", "John Doe");

        List<TagSet> tagSets = new ArrayList<>();
        TagSet tagSet = new TagSet();
        tagSet.setTag(tag.getKey(), tag.getValue());
        tagSets.add(tagSet);

        BucketTaggingConfiguration configuration = new BucketTaggingConfiguration();
        configuration.setTagSets(tagSets);

        // when
        when(s3.getBucketTaggingConfiguration(bucket))
                .thenReturn(configuration);

        boolean result = s3Handler.bucketHasTag(bucket, "Creator");

        // then
        assertEquals(false, result);
    }
}
