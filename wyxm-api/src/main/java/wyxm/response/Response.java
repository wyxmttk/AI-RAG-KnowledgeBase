package wyxm.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    private String code;
    private String info;
    private T data;

    public static Response<String> success() {
        return Response.<String>builder().code("0000").info("success").build();
    }

    public static <T> Response<T> success(T data) {
        return Response.<T>builder().code("0000").info("success").data(data).build();
    }

}
