package org.superbiz.moviefun.storage;


import com.amazonaws.util.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;

@Service
public class FileStore implements BlobStore {
    @Override
    public void put(Blob blob) throws IOException {

        File file = new File(blob.name);
        if(file.exists()){
            file.delete();
            file.getParentFile().mkdirs();
            file.createNewFile();
        }


        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(IOUtils.toByteArray(blob.inputStream));
        }
    }

    @Override
    public Optional<Blob> get(String name) throws IOException {

        File coverFile = new File(name);

        if (coverFile.exists()) {
            return Optional.of(new Blob(name, new FileInputStream(coverFile), MediaType.IMAGE_JPEG_VALUE));
        } else {
            return Optional.empty();
        }

    }

    @Override
    public void clean() {

    }

    private File getCoverFile(long albumId) {
        String coverFileName = format("covers/%d", albumId);
        return new File(coverFileName);
    }
}
