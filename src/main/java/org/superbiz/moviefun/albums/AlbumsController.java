package org.superbiz.moviefun.albums;


import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.superbiz.moviefun.storage.Blob;
import org.superbiz.moviefun.storage.BlobStore;
import org.superbiz.moviefun.storage.FileStore;
import org.superbiz.moviefun.storage.S3Store;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Controller
@RequestMapping("/albums")
public class AlbumsController {

    Logger logger = LoggerFactory.getLogger(getClass());

    private final AlbumsBean albumsBean;
    private final FileStore fileStore;
    private final BlobStore s3Store;

    public AlbumsController(AlbumsBean albumsBean, FileStore fileStore, BlobStore s3Store) {
        this.albumsBean = albumsBean;
        this.fileStore = fileStore;
        this.s3Store = s3Store;
    }


    @GetMapping
    public String index(Map<String, Object> model) {
        model.put("albums", albumsBean.getAlbums());
        return "albums";
    }

    @GetMapping("/{albumId}")
    public String details(@PathVariable long albumId, Map<String, Object> model) {
        logger.info("Show details of albumId: " + albumId);
        model.put("album", albumsBean.find(albumId));
        return "albumDetails";
    }

    @PostMapping("/{albumId}/cover")
    public String uploadCover(@PathVariable long albumId, @RequestParam("file") MultipartFile uploadedFile) throws IOException {
        //saveUploadToFile(uploadedFile, fileStore.getCoverFile(albumId));

        logger.info("Upload cover image for albumId: " + albumId);

        Blob blob = new Blob(String.valueOf(albumId),
                uploadedFile.getInputStream(),
                uploadedFile.getContentType());

        s3Store.put(blob);

        return format("redirect:/albums/%d", albumId);
    }

    @GetMapping("/{albumId}/cover")
    public HttpEntity<byte[]> getCover(@PathVariable long albumId) throws IOException, URISyntaxException {
        logger.info("Get cover image for albumId: " + albumId);

        String coverFile        = getCoverFileName(albumId);
        //byte[] imageBytes     = readAllBytes(coverFilePath);

        Optional<Blob> opBlob   = s3Store.get(coverFile);
        Blob blob               = opBlob.orElse(getExistingCover(albumId));

        byte[] imageBytes       = IOUtils.toByteArray(blob.inputStream);

        //HttpHeaders headers   = createImageHttpHeaders(coverFilePath, imageBytes);
        HttpHeaders headers     = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(blob.contentType));
        headers.setContentLength(imageBytes.length);

        return new HttpEntity<>(imageBytes, headers);
    }



    private void saveUploadToFile(@RequestParam("file") MultipartFile uploadedFile, File targetFile) throws IOException {
        /*targetFile.delete();
        targetFile.getParentFile().mkdirs();
        targetFile.createNewFile();

        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            outputStream.write(uploadedFile.getBytes());
        }*/
    }

    /*private HttpHeaders createImageHttpHeaders(Path coverFilePath, byte[] imageBytes) throws IOException {
        String contentType = new Tika().detect(coverFilePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(imageBytes.length);
        return headers;
    }*/

    private String getCoverFileName(long albumId) {
        return String.valueOf(albumId);
    }

    private Blob getExistingCover(long albumId) throws URISyntaxException {
        /*File coverFile = fileStore.getCoverFile(albumId);
        Path coverFilePath;

        if (coverFile.exists()) {
            coverFilePath = coverFile.toPath();
        } else {
            coverFilePath = Paths.get(getSystemResource("default-cover.jpg").toURI());
        }*/
        ClassLoader classLoader = getClass().getClassLoader();
        String coverFileName    = getCoverFileName(albumId);
        Blob blob = new Blob(coverFileName,
                classLoader.getResourceAsStream("default-cover.jpg"),
                MediaType.IMAGE_JPEG_VALUE);
        return blob;
    }
}
