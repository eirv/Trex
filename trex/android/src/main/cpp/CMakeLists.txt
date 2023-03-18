cmake_minimum_required(VERSION 3.4.1)

project(trex)

set(C_FLAGS "-Wno-builtin-macro-redefined -D__FILE__=__FILE_NAME__")
set(CXX_FLAGS "-fno-exceptions -fno-rtti")
set(LINKER_FLAGS "-Wl,-exclude-libs,ALL")

set(C_FLAGS "${C_FLAGS} -fdata-sections -ffunction-sections -fvisibility=hidden -fvisibility-inlines-hidden -Oz -Wno-macro-redefined -DLOGGING_DISABLE=ON")
set(CXX_FLAGS "${CXX_FLAGS} -fno-stack-protector -fomit-frame-pointer -flto=thin -fno-unwind-tables -fno-asynchronous-unwind-tables")
set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections -Wl,--strip-all -Wl,--icf=safe -Wl,-O3")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_FLAGS} ${CXX_FLAGS}")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

add_library(trex SHARED trex.cpp elf_img.cpp)

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

if(NOT TARGET dobby)
set(DOBBY_DIR "${CMAKE_SOURCE_DIR}/dobby")
macro(SET_OPTION option value)
  set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()
SET_OPTION(DOBBY_DEBUG OFF)
SET_OPTION(DOBBY_GENERATE_SHARED OFF)
add_subdirectory(${DOBBY_DIR} dobby)
get_property(DOBBY_INCLUDE_DIRECTORIES
  TARGET dobby
  PROPERTY INCLUDE_DIRECTORIES)
include_directories(
  .
  ${DOBBY_INCLUDE_DIRECTORIES}
  $<TARGET_PROPERTY:dobby,INCLUDE_DIRECTORIES>
)
endif()

target_link_libraries(trex log dobby)

add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:${PROJECT_NAME}>)
