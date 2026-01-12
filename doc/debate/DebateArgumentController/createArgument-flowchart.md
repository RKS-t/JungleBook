# DebateArgumentController.createArgument - Flowchart

```mermaid
flowchart TD
    Start([Client Request: POST /api/debate/topics/:topicId/arguments]) --> AuthCheck{Authentication<br/>Required?}
    AuthCheck -->|Yes| JWTValidate[JWT Token Validation<br/><i>JwtAuthenticationFilter</i>]
    AuthCheck -->|No| GlobalExceptionHandler1[GlobalExceptionHandler<br/>Return 401]
    
    JWTValidate --> TokenValid{Token Valid?}
    TokenValid -->|No| GlobalExceptionHandler2[GlobalExceptionHandler<br/>Return 401]
    TokenValid -->|Yes| ExtractMember[Extract Member from Token]
    
    ExtractMember --> GetMemberId[Get Member ID from MemberService]
    GetMemberId --> ValidateContent[Validate Content Length<br/><i>JBConstants.DEBATE_ARGUMENT_MAX_CONTENT_LENGTH</i>]
    
    ValidateContent --> ContentValid{Content Length<br/>Valid?}
    ContentValid -->|No| GlobalExceptionHandler3[GlobalExceptionHandler<br/>Return 400<br/>WRONG_ACCESS]
    ContentValid -->|Yes| CreateArgumentEntity[Create DebateArgumentEntity<br/><i>request.toEntity(topicId, userId)</i>]
    
    CreateArgumentEntity --> SaveArgument[Save Argument to Database<br/><i>DebateArgumentRepository.save()</i>]
    SaveArgument --> ProcessFiles{File IDs<br/>Exist?}
    ProcessFiles -->|Yes| UpdateFiles[Update File Attach Status<br/><i>DebateFileRepository.updateAttachStatus()<br/>For each fileId</i>]
    ProcessFiles -->|No| IncreaseTopicCount
    UpdateFiles --> IncreaseTopicCount[Increase Topic Argument Count<br/><i>DebateTopicService.increaseArgumentCount()</i>]
    IncreaseTopicCount --> GetTopic[Get Topic Entity<br/><i>DebateTopicRepository.findByIdAndActiveYnTrue()</i>]
    GetTopic --> DetectFallacy[Detect Logical Fallacy Async<br/><i>FallacyDetectionService.detectFallacyAsync()<br/>with orTimeout()</i>]
    DetectFallacy --> CreateResponse[Create DebateArgumentResponse<br/><i>DebateArgumentResponse.of()<br/>Immediate return</i>]
    CreateResponse --> Return201[Return 201 Created]
    
    DetectFallacy -.->|Async Callback| AsyncProcess[Async Callback Processing<br/>Non-blocking]
    AsyncProcess --> TimeoutCheck{Timeout<br/>Exception?}
    TimeoutCheck -->|Yes| LogTimeout[Log Timeout Warning<br/><i>logger().warn()</i>]
    TimeoutCheck -->|No| ProcessResult{Result<br/>Available?}
    ProcessResult -->|Yes| UpdateFallacy[Update Fallacy Fields<br/><i>TransactionTemplate<br/>DebateArgumentRepository</i>]
    ProcessResult -->|No| LogError[Log Error<br/><i>logger().error()</i>]
    UpdateFallacy --> LogSuccess[Log Success<br/><i>logger().info()</i>]
    LogTimeout --> End
    LogError --> End
    LogSuccess --> End
    
    Return201 --> End([End])
    GlobalExceptionHandler1 --> End
    GlobalExceptionHandler2 --> End
    GlobalExceptionHandler3 --> End
    
    style Start fill:#e1f5ff
    style End fill:#e1f5ff
    style GlobalExceptionHandler1 fill:#ffebee
    style GlobalExceptionHandler2 fill:#ffebee
    style GlobalExceptionHandler3 fill:#ffebee
    style Return201 fill:#e8f5e9
```
