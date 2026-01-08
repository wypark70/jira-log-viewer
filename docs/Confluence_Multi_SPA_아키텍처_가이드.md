Confluence Data Center 환경에서 **"일반 사용자용 앱(User App)"**과 **"관리자용 앱(Admin App)"**을 물리적으로 분리하여 개발하는 Multi-SPA (Multiple Single Page Application) 전체 아키텍처를 정리해 드립니다.
이 가이드를 따라가면 하나의 플러그인 프로젝트 안에서 두 개의 독립적인 React 앱이 돌아가는 구조가 완성됩니다.
🏗️ 1. 전체 프로젝트 구조 (Directory Structure)
Java와 React가 공존하는 구조입니다. frontend 폴더 안에서 앱이 두 갈래로 나뉘는 것이 핵심입니다.
```tree
my-confluence-plugin/
├── pom.xml                       # [Maven] 빌드 자동화 설정
├── src/
│   └── main/
│       ├── java/                 # [Java] REST API & Action
│       └── resources/            # [XML] 설정 파일
│           ├── atlassian-plugin.xml
│           ├── templates/        # [View] 껍데기 HTML
│           │   ├── user-view.vm  # (User App용)
│           │   └── admin-view.vm # (Admin App용)
│           └── js/               # [Target] Webpack 빌드 결과물이 여기로 떨어짐
└── frontend/                     # [React] 프론트엔드 소스
    ├── package.json
    ├── webpack.config.js         # [Webpack] 멀티 엔트리 설정
    └── src/
        ├── apps/
        │   ├── user/             # [App 1] 일반 사용자용 앱
        │   │   ├── index.js      # Entry Point
        │   │   └── App.js        # React Router
        │   └── admin/            # [App 2] 관리자용 앱
        │       ├── index.js      # Entry Point
        │       └── App.js        # React Router
        └── components/           # 공통 컴포넌트
```
🎨 2. Frontend 설정 (React & Webpack)
두 개의 JS 번들을 만들어내기 위한 설정입니다.
2-1. frontend/webpack.config.js
entry를 객체로 정의하여 두 개의 번들을 생성합니다.
```js
const path = require('path');

module.exports = {
  // [핵심] 1. 진입점 분리 (Key값이 파일명이 됨)
  entry: {
    userApp: './src/apps/user/index.js',   // -> userApp.bundle.js
    adminApp: './src/apps/admin/index.js'  // -> adminApp.bundle.js
  },
  output: {
    // [핵심] 2. Java 리소스 폴더로 자동 출력
    path: path.resolve(__dirname, '../src/main/resources/js'),
    filename: '[name].bundle.js' // [name]에 userApp, adminApp이 들어감
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: ['babel-loader']
      },
      { test: /\.css$/, use: ['style-loader', 'css-loader'] }
    ]
  }
};
```
2-2. React Entry Points
각 앱은 서로 다른 DOM ID(root-user vs root-admin)에 렌더링됩니다.
User App (src/apps/user/index.js):
```js
import React from 'react';
import ReactDOM from 'react-dom';
import Button from '@atlaskit/button';

const UserApp = () => (
  <div style={{ padding: '20px' }}>
    <h2>🙋‍♂️ 일반 사용자 페이지</h2>
    <Button appearance="primary">신청하기</Button>
  </div>
);

document.addEventListener('DOMContentLoaded', () => {
  const root = document.getElementById('root-user'); // ID 주의
  if (root) ReactDOM.render(<UserApp />, root);
});
```
Admin App (src/apps/admin/index.js):
```js
import React from 'react';
import ReactDOM from 'react-dom';
import Button from '@atlaskit/button';

const AdminApp = () => (
  <div style={{ padding: '20px', backgroundColor: '#f4f5f7' }}>
    <h2>⚙️ 관리자 설정 페이지</h2>
    <Button appearance="danger">시스템 초기화</Button>
  </div>
);

document.addEventListener('DOMContentLoaded', () => {
  const root = document.getElementById('root-admin'); // ID 주의
  if (root) ReactDOM.render(<AdminApp />, root);
});
```

🔗 3. Backend 통합 (XML & Template)
생성된 두 개의 JS 파일을 각기 다른 URL에서 로드하도록 연결합니다.
3-1. atlassian-plugin.xml
```xml
<atlassian-plugin key="${atlassian.plugin.key}" plugins-version="2" name="${project.name}">

    <web-resource key="res-user" name="User Resources">
        <resource type="download" name="userApp.bundle.js" location="/js/userApp.bundle.js"/>
        <context>atl.general</context>
    </web-resource>

    <xwork key="action-user" name="User Action">
        <package name="user-pkg" extends="default" namespace="/plugins/myapp">
            <action name="dashboard" class="com.example.ViewAction">
                <result name="success" type="velocity">/templates/user-view.vm</result>
            </action>
        </package>
    </xwork>


    <web-resource key="res-admin" name="Admin Resources">
        <resource type="download" name="adminApp.bundle.js" location="/js/adminApp.bundle.js"/>
        <context>atl.admin</context> </web-resource>

    <xwork key="action-admin" name="Admin Action">
        <package name="admin-pkg" extends="default" namespace="/plugins/myapp/admin">
            <action name="config" class="com.example.ViewAction">
                <result name="success" type="velocity">/templates/admin-view.vm</result>
            </action>
        </package>
    </xwork>

    <rest key="my-rest" path="/myapp" version="1.0"/>

</atlassian-plugin>
```
3-2. Velocity Templates (HTML 껍데기)
/templates/user-view.vm
```vm
<html>
<head>
    <title>User Dashboard</title>
    <meta name="decorator" content="atl.general"/>
    $webResourceManager.requireResource("${atlassian.plugin.key}:res-user")
</head>
<body>
    <div id="root-user"></div> </body>
</html>

/templates/admin-view.vm
<html>
<head>
    <title>Admin Config</title>
    <meta name="decorator" content="atl.admin"/>
    $webResourceManager.requireResource("${atlassian.plugin.key}:res-admin")
</head>
<body>
    <div id="root-admin"></div> </body>
</html>
```
3-3. Java Action (ViewAction.java)
단순히 뷰만 연결해주면 되므로 하나로 공유해도 됩니다.
```java
package com.example;
import com.atlassian.confluence.core.ConfluenceActionSupport;

public class ViewAction extends ConfluenceActionSupport {
    @Override
    public String execute() throws Exception {
        return SUCCESS; // XML에 정의된 vm 파일 렌더링
    }
}
```
📡 4. REST API (데이터 통신)
두 앱이 공통으로 혹은 각각 사용할 데이터를 제공합니다.
```java
package com.example.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/data")
public class MyResource {

    // User App용 데이터
    @GET
    @Path("/user")
    @Produces("application/json")
    public Response getUserData() {
        return Response.ok("{\"type\":\"user\", \"msg\":\"Hello User\"}").build();
    }

    // Admin App용 데이터 (관리자 권한 체크 필요)
    @GET
    @Path("/admin")
    @Produces("application/json")
    public Response getAdminData() {
        // 실무에선 여기서 Permission Check 필수
        return Response.ok("{\"type\":\"admin\", \"msg\":\"System Status OK\"}").build();
    }
}
```
🤖 5. 빌드 자동화 (pom.xml)
Maven 빌드 시 React 빌드까지 한 번에 끝내기 위한 설정입니다.
```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.12.0</version>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
        <installDirectory>target</installDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install node and npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration>
                <nodeVersion>v14.17.0</nodeVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>install</arguments></configuration>
        </execution>
        <execution>
            <id>npm run build</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>run build</arguments></configuration>
        </execution>
    </executions>
</plugin>
```
🏁 최종 정리: 개발 및 실행 흐름
 * 개발: frontend 폴더에서 React 코드를 수정합니다.
 * 빌드: 터미널에서 atlas-package를 입력합니다.
   * Maven이 frontend-maven-plugin을 실행합니다.
   * Webpack이 userApp.bundle.js, adminApp.bundle.js를 생성하여 Java 리소스 폴더에 넣습니다.
   * Java 플러그인이 .jar로 패키징됩니다.
 * 실행 (User): /plugins/myapp/dashboard.action 접속 -> user-view.vm -> userApp.bundle.js 실행.
 * 실행 (Admin): /plugins/myapp/admin/config.action 접속 -> admin-view.vm -> adminApp.bundle.js 실행.
이 구조는 관리자/사용자 기능이 명확히 분리되고, 최신 React 기술을 사용할 수 있는 가장 깔끔한 방법입니다.
