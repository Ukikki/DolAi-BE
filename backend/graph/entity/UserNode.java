package graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.*;
import org.springframework.data.annotation.Id;

@Document("users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNode {
    @Id
    private String id;
    private String name;
    private String email;
}