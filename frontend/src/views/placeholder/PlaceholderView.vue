<template>
  <div class="flex flex-col items-center justify-center h-full min-h-[60vh]">
    <span class="material-symbols-outlined text-6xl text-slate-300 mb-4">construction</span>
    <p class="text-slate-400 text-lg font-medium">该功能模块正在开发中...</p>
    <button
      @click="$router.push('/chat')"
      class="mt-6 text-primary text-sm font-medium hover:underline"
    >
      返回仪表盘
    </button>
  </div>
</template>
