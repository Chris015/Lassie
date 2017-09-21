package lassie.awsHandlers;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.TagSet;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class S3HandlerTest {

    private AmazonS3Client s3;
    private S3Handler s3Handler;

    @Before
    public void setUp() throws Exception {
        this.s3 = mock(AmazonS3Client.class);
        this.s3Handler = mock(S3Handler.class);
    }

    @Test
    public void bucketHasTagReturnsTrue() throws Exception {
        String bucket = "Bucket";
        Tag tag = new Tag("Owner", "John Doe");

        List<TagSet> tagSets = new ArrayList<>();
        TagSet tagSet = new TagSet();
        tagSet.setTag(tag.getKey(), tag.getValue());
        tagSets.add(tagSet);

        BucketTaggingConfiguration configuration = new BucketTaggingConfiguration(tagSets);

        when(s3.getBucketTaggingConfiguration(bucket)).thenReturn(configuration);
        when(s3Handler.bucketHasTag(bucket, "Owner")).thenReturn(true);

        assertEquals(true, s3Handler.bucketHasTag(bucket, "Owner"));
    }

    @Test
    public void bucketHasTagReturnsFalse() throws Exception {
        String bucket = "Bucket";
        Tag tag = new Tag("Creator", "John Doe");

        List<TagSet> tagSets = new ArrayList<>();
        TagSet tagSet = new TagSet();
        tagSet.setTag(tag.getKey(), tag.getValue());
        tagSets.add(tagSet);

        BucketTaggingConfiguration configuration = new BucketTaggingConfiguration(tagSets);

        when(s3.getBucketTaggingConfiguration(bucket)).thenReturn(configuration);
        when(s3Handler.bucketHasTag(bucket, "Owner")).thenReturn(false);

        assertEquals(false, s3Handler.bucketHasTag(bucket, "Owner"));
    }
}
