package org.hgc.suts.picture.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hamcrest.StringDescription;
import org.springframework.web.multipart.MultipartFile;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadPictureAnalysisReqDTO {

    /**
     * 图片
     */
    MultipartFile multipartFile;

    /**
     * 用户描述内容
     */
    String descriptionContent;
}
