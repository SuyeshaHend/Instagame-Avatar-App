// uploader.js - Improved version that handles both Ktor and standard multipart formats
var uploader_default = {
  async fetch(request, env) {
    const url = new URL(request.url);
    const clientKey = request.headers.get("x-api-key");
    
    if (clientKey !== env.API_KEY) {
      return new Response(JSON.stringify({
        success: false,
        message: "Unauthorized: Invalid or missing API key"
      }), {
        status: 401,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*"
        }
      });
    }
    
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, DELETE, GET, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, x-api-key"
        }
      });
    }
    
    if (request.method === "POST") {
      const contentType = request.headers.get("content-type") || "";
      if (!contentType.includes("multipart/form-data")) {
        return new Response("Expected multipart/form-data", {
          status: 400,
          headers: { "Access-Control-Allow-Origin": "*" }
        });
      }
      
      try {
        const formData = await request.formData();
        
        // Try to find file in multiple ways to support different clients
        let file = null;
        let fileFieldName = null;
        
        // Method 1: Look for File instance (standard browsers, cURL, Retrofit)
        for (const [key, value] of formData.entries()) {
          if (value instanceof File) {
            file = value;
            fileFieldName = key;
            break;
          }
        }
        
        // Method 2: If no File found, look for Blob or any binary data with "file" key
        if (!file) {
          const fileEntry = formData.get("file");
          if (fileEntry && (fileEntry instanceof Blob || fileEntry instanceof File)) {
            file = fileEntry;
            fileFieldName = "file";
          }
        }
        
        if (!file) {
          return new Response(JSON.stringify({
            success: false,
            message: "No file uploaded",
            debug: {
              formDataKeys: Array.from(formData.keys()),
              contentType: contentType
            }
          }), {
            status: 400,
            headers: { 
              "Content-Type": "application/json",
              "Access-Control-Allow-Origin": "*" 
            }
          });
        }
        
        let path = formData.get("path") || "";
        let name = formData.get("name") || "";
        
        // If name not provided, try to get from file.name
        if (!name) {
          name = file.name || "unnamed_file";
        }
        
        path = path.replace(/^\/+/, "").replace(/\/+$/, "");
        const key = path ? `${path}/${name}` : name;
        
        await env.LINK_BUCKET.put(key, await file.arrayBuffer(), {
          httpMetadata: { 
            contentType: file.type || "application/octet-stream" 
          }
        });
        
        const publicUrl = `https://pub-22db73b8d33244d1a53831aed22cd78b.r2.dev/${key}`;
        
        return new Response(JSON.stringify({
          success: true,
          url: publicUrl,
          key,
          name,
          path
        }), {
          headers: { 
            "Content-Type": "application/json", 
            "Access-Control-Allow-Origin": "*" 
          }
        });
        
      } catch (err) {
        return new Response(JSON.stringify({
          success: false,
          message: "Error processing upload: " + err.message,
          error: err.toString()
        }), {
          status: 500,
          headers: { 
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*" 
          }
        });
      }
      
    } else if (request.method === "DELETE") {
      let rawPath = "";
      const contentType = request.headers.get("content-type") || "";
      
      if (contentType.includes("multipart/form-data")) {
        const formData = await request.formData();
        rawPath = formData.get("path") || "";
      } else if (contentType.includes("application/json")) {
        try {
          const body = await request.json();
          rawPath = body?.path || "";
        } catch {
          rawPath = "";
        }
      }
      
      if (!rawPath) rawPath = url.searchParams.get("path") || "";
      rawPath = (rawPath || "").replace(/^\/+/, "").replace(/\/+$/, "");
      
      if (!rawPath) {
        return new Response(JSON.stringify({
          success: false,
          message: "Missing 'path' parameter"
        }), { 
          status: 400,
          headers: { 
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*" 
          }
        });
      }
      
      try {
        let deletedFiles = [];
        let cursor;
        
        do {
          const listResponse = await env.LINK_BUCKET.list({ 
            prefix: rawPath, 
            cursor 
          });
          cursor = listResponse.truncated ? listResponse.cursor : null;
          
          for (const obj of listResponse.objects) {
            await env.LINK_BUCKET.delete(obj.key);
            deletedFiles.push(obj.key);
          }
        } while (cursor);
        
        if (deletedFiles.length === 0) {
          return new Response(JSON.stringify({
            success: false,
            message: "No files found for the given path"
          }), {
            headers: { 
              "Content-Type": "application/json", 
              "Access-Control-Allow-Origin": "*" 
            }
          });
        }
        
        return new Response(JSON.stringify({
          success: true,
          deleted: deletedFiles
        }), {
          headers: { 
            "Content-Type": "application/json", 
            "Access-Control-Allow-Origin": "*" 
          }
        });
        
      } catch (err) {
        return new Response(JSON.stringify({
          success: false,
          message: err.message
        }), {
          status: 500,
          headers: { 
            "Content-Type": "application/json", 
            "Access-Control-Allow-Origin": "*" 
          }
        });
      }
      
    } else if (request.method === "GET") {
      let folderPath = "";
      const contentType = request.headers.get("content-type") || "";
      
      if (contentType.includes("multipart/form-data")) {
        const formData = await request.formData();
        folderPath = formData.get("path") || "";
      } else if (contentType.includes("application/json")) {
        try {
          const body = await request.json();
          folderPath = body?.path || "";
        } catch {
          folderPath = "";
        }
      }
      
      if (!folderPath) folderPath = url.searchParams.get("path") || "";
      folderPath = (folderPath || "").replace(/^\/+/, "").replace(/\/+$/, "");
      
      if (typeof folderPath !== "string") folderPath = "";
      
      let cursor;
      let files = [];
      
      try {
        do {
          const listResponse = await env.LINK_BUCKET.list({ 
            prefix: folderPath, 
            cursor 
          });
          cursor = listResponse.truncated ? listResponse.cursor : null;
          
          files.push(...listResponse.objects.map((obj) => ({
            key: obj.key,
            size: obj.size,
            etag: obj.etag
          })));
        } while (cursor);
        
        return new Response(JSON.stringify({
          success: true,
          files
        }), {
          headers: { 
            "Content-Type": "application/json", 
            "Access-Control-Allow-Origin": "*" 
          }
        });
        
      } catch (err) {
        return new Response(JSON.stringify({
          success: false,
          message: err.message
        }), {
          status: 500,
          headers: { 
            "Content-Type": "application/json", 
            "Access-Control-Allow-Origin": "*" 
          }
        });
      }
      
    } else {
      return new Response("Method Not Allowed", { status: 405 });
    }
  }
};

export { uploader_default as default };
