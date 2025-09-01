#!/usr/bin/env node

/**
 * Test MCP Server
 * A simple Model Context Protocol server that provides current time functionality
 * Used for testing MCP integration in the AlIna Desktop AI Assistant
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';

class TestTimeServer {
  constructor() {
    this.server = new Server(
      {
        name: 'test-time-server',
        version: '1.0.0',
      },
      {
        capabilities: {
          tools: {},
        },
      }
    );

    this.setupToolHandlers();
    this.setupErrorHandling();
  }

  setupToolHandlers() {
    // Handle tool listing
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      return {
        tools: [
          {
            name: 'get_current_time',
            description: 'Get the current date and time in ISO format',
            inputSchema: {
              type: 'object',
              properties: {},
              required: [],
            },
          },
          {
            name: 'get_current_time_formatted',
            description: 'Get the current date and time in a human-readable format',
            inputSchema: {
              type: 'object',
              properties: {
                timezone: {
                  type: 'string',
                  description: 'Timezone for the formatted time (e.g., "America/New_York", "Europe/London")',
                  default: 'local'
                },
                format: {
                  type: 'string',
                  description: 'Format style: "short", "medium", "long", or "full"',
                  default: 'medium'
                }
              },
              required: [],
            },
          },
          {
            name: 'get_timestamp',
            description: 'Get the current Unix timestamp',
            inputSchema: {
              type: 'object',
              properties: {
                format: {
                  type: 'string',
                  description: 'Timestamp format: "seconds" or "milliseconds"',
                  default: 'milliseconds'
                }
              },
              required: [],
            },
          }
        ],
      };
    });

    // Handle tool calls
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const { name, arguments: args } = request.params;

      try {
        switch (name) {
          case 'get_current_time':
            return await this.getCurrentTime();
            
          case 'get_current_time_formatted':
            return await this.getCurrentTimeFormatted(args);
            
          case 'get_timestamp':
            return await this.getTimestamp(args);
            
          default:
            throw new Error(`Unknown tool: ${name}`);
        }
      } catch (error) {
        // Log errors to stderr
        console.error(`Error in tool ${name}:`, error);
        return {
          content: [
            {
              type: 'text',
              text: `Error executing ${name}: ${error.message}`
            }
          ],
          isError: true,
        };
      }
    });
  }

  async getCurrentTime() {
    const now = new Date();
    return {
      content: [
        {
          type: 'text',
          text: `Current time: ${now.toISOString()}`
        }
      ],
    };
  }

  async getCurrentTimeFormatted(args = {}) {
    const now = new Date();
    const timezone = args.timezone || 'local';
    const format = args.format || 'medium';

    let formattedTime;
    
    try {
      const options = {
        year: 'numeric',
        month: format === 'short' ? 'numeric' : format === 'long' ? 'long' : 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: format === 'long' || format === 'full' ? '2-digit' : undefined,
        timeZoneName: format === 'full' ? 'long' : format === 'long' ? 'short' : undefined,
        timeZone: timezone === 'local' ? undefined : timezone,
      };

      formattedTime = now.toLocaleString('en-US', options);
    } catch (error) {
      formattedTime = now.toString();
    }

    return {
      content: [
        {
          type: 'text',
          text: `Current time (${timezone}, ${format}): ${formattedTime}`
        }
      ],
    };
  }

  async getTimestamp(args = {}) {
    const format = args.format || 'milliseconds';
    const now = new Date();
    
    const timestamp = format === 'seconds' ? Math.floor(now.getTime() / 1000) : now.getTime();
    
    return {
      content: [
        {
          type: 'text',
          text: `Current timestamp (${format}): ${timestamp}`
        }
      ],
    };
  }

  setupErrorHandling() {
    this.server.onerror = (error) => {
      // Log errors to stderr instead of stdout to avoid JSON-RPC parsing issues
      console.error('[MCP Error]', error);
    };

    process.on('SIGINT', async () => {
      // Log to stderr instead of stdout
      console.error('\n[Test MCP Server] Shutting down...');
      await this.server.close();
      process.exit(0);
    });
  }

  async start() {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    // Don't log startup message to stdout as it interferes with MCP protocol
    // Log to stderr instead
    console.error('[Test MCP Server] Started successfully');
  }
}

// Start the server
const server = new TestTimeServer();
server.start().catch((error) => {
  // Log errors to stderr
  console.error('[Test MCP Server] Failed to start:', error);
  process.exit(1);
});